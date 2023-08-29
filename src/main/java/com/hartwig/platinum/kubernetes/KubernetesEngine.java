package com.hartwig.platinum.kubernetes;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.container.v1beta1.Container;
import com.google.api.services.container.v1beta1.Container.Projects.Locations.Clusters.Create;
import com.google.api.services.container.v1beta1.Container.Projects.Locations.Clusters.Get;
import com.google.api.services.container.v1beta1.model.*;
import com.hartwig.pdl.PipelineInput;
import com.hartwig.platinum.Console;
import com.hartwig.platinum.config.BatchConfiguration;
import com.hartwig.platinum.config.GcpConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.config.ServiceAccountConfiguration;
import com.hartwig.platinum.kubernetes.pipeline.PipelineConfigMapBuilder;
import com.hartwig.platinum.kubernetes.pipeline.PipelineConfigMapVolume.PipelineConfigMapVolumeBuilder;
import com.hartwig.platinum.scheduling.JobScheduler;
import joptsimple.internal.Strings;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

public class KubernetesEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesEngine.class);
    private final Container containerApi;
    private final ProcessRunner processRunner;
    private final PlatinumConfiguration configuration;
    private final JobScheduler jobScheduler;
    private final KubernetesClientProxy kubernetesClientProxy;

    public KubernetesEngine(final Container containerApi, final ProcessRunner processRunner, final PlatinumConfiguration configuration,
            final JobScheduler jobScheduler, final KubernetesClientProxy kubernetesClientProxy) {
        this.containerApi = containerApi;
        this.processRunner = processRunner;
        this.configuration = configuration;
        this.jobScheduler = jobScheduler;
        this.kubernetesClientProxy = kubernetesClientProxy;
    }

    private static String fullPath(final String project, final String region, final String cluster) {
        return String.format("projects/%s/locations/%s/clusters/%s", project, region, cluster);
    }

    private static void create(final Container containerApi, final String parent, final String cluster,
            final GcpConfiguration gcpConfiguration) {
        try {
            Cluster newCluster = new Cluster();
            newCluster.setName(cluster);
            newCluster.setNetwork(gcpConfiguration.networkUrl());
            newCluster.setSubnetwork(gcpConfiguration.subnetUrl());
            newCluster.setLocations(gcpConfiguration.zones());
            NodePool defaultNodePool = new NodePool().setName("default").setInitialNodeCount(2);
            final NodeConfig nodeConfig = new NodeConfig().setPreemptible(gcpConfiguration.preemptibleCluster())
                    .setOauthScopes(List.of("https://www.googleapis.com/auth/cloud-platform"))
                    .setDiskSizeGb(500);
            if (!gcpConfiguration.networkTags().isEmpty()) {
                nodeConfig.setTags(gcpConfiguration.networkTags());
            }
            defaultNodePool.setConfig(nodeConfig);
            newCluster.setNodePools(List.of(defaultNodePool));

            IPAllocationPolicy ipAllocationPolicy = new IPAllocationPolicy();
            if (gcpConfiguration.privateCluster()) {
                PrivateClusterConfig privateClusterConfig = new PrivateClusterConfig();
                privateClusterConfig.setEnablePrivateEndpoint(true);
                privateClusterConfig.setEnablePrivateNodes(true);
                privateClusterConfig.setMasterIpv4CidrBlock(gcpConfiguration.masterIpv4CidrBlock());
                newCluster.setPrivateCluster(true);
                newCluster.setPrivateClusterConfig(privateClusterConfig);
                ipAllocationPolicy.setUseIpAliases(true);
            }
            if (gcpConfiguration.secondaryRangeNamePods().isPresent() && gcpConfiguration.secondaryRangeNameServices().isPresent()) {
                ipAllocationPolicy.setClusterSecondaryRangeName(gcpConfiguration.secondaryRangeNamePods().get());
                ipAllocationPolicy.setServicesSecondaryRangeName(gcpConfiguration.secondaryRangeNameServices().get());
            }
            newCluster.setIpAllocationPolicy(ipAllocationPolicy);

            CreateClusterRequest createRequest = new CreateClusterRequest();
            createRequest.setCluster(newCluster);
            Create created = containerApi.projects().locations().clusters().create(parent, createRequest);
            Operation execute = created.execute();
            LOGGER.info("Creating new kubernetes cluster {} in project {} and region {}, this can take upwards of 5 minutes...",
                    Console.bold(newCluster.getName()),
                    Console.bold(gcpConfiguration.projectOrThrow()),
                    Console.bold(gcpConfiguration.regionOrThrow()));
            Failsafe.with(new RetryPolicy<>().withMaxDuration(ofMinutes(15))
                            .withDelay(ofSeconds(15))
                            .withMaxAttempts(-1)
                            .handleResult(null)
                            .handleResult("RUNNING"))
                    .onFailure(objectExecutionCompletedEvent -> LOGGER.info("Waiting on operation, status is [{}]",
                            objectExecutionCompletedEvent.getResult()))
                    .get(() -> containerApi.projects()
                            .locations()
                            .operations()
                            .get(String.format("projects/%s/locations/%s/operations/%s",
                                    gcpConfiguration.projectOrThrow(),
                                    gcpConfiguration.regionOrThrow(),
                                    execute.getName()))
                            .execute()
                            .getStatus());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create cluster", e);
        }
    }

    private Optional<Cluster> find(final String path) throws IOException {
        try {
            Get found = containerApi.projects().locations().clusters().get(path);
            return Optional.of(found.execute());
        } catch (GoogleJsonResponseException e) {
            return Optional.empty();
        }
    }

    public KubernetesCluster findOrCreate(final String clusterName, final String runName,
            final List<Supplier<PipelineInput>> pipelineInputs, final String outputBucketName) {
        try {
            GcpConfiguration gcpConfiguration = configuration.gcp();
            String parent = String.format("projects/%s/locations/%s", gcpConfiguration.projectOrThrow(), gcpConfiguration.regionOrThrow());
            if (find(fullPath(gcpConfiguration.projectOrThrow(), gcpConfiguration.regionOrThrow(), clusterName)).isEmpty()) {
                create(containerApi, parent, clusterName, gcpConfiguration);
            }
            if (!configuration.inCluster()) {
                kubernetesClientProxy.authorise();
                LOGGER.info("Connection to cluster {} configured via gcloud and kubectl", Console.bold(clusterName));
            }

            TargetNodePool targetNodePool = configuration.gcp()
                    .nodePoolConfiguration()
                    .map(c -> TargetNodePool.fromConfig(c,
                            configuration.batch()
                                    .map(BatchConfiguration::size)
                                    .orElse(configuration.samples().isEmpty()
                                            ? configuration.sampleIds().size()
                                            : configuration.samples().size())))
                    .orElse(TargetNodePool.defaultPool());
            String serviceAccountEmail = configuration.serviceAccount().flatMap(ServiceAccountConfiguration::gcpEmailAddress).orElse(Strings.EMPTY);
            if (!targetNodePool.isDefault()) {
                new GcloudNodePool(processRunner).create(targetNodePool,
                        serviceAccountEmail,
                        clusterName,
                        gcpConfiguration.projectOrThrow());
            }
            return new KubernetesCluster(runName,
                    jobScheduler,
                    pipelineInputs,
                    new PipelineConfigMapBuilder(new PipelineConfigMapVolumeBuilder(kubernetesClientProxy), runName),
                    outputBucketName,
                    serviceAccountEmail,
                    configuration,
                    targetNodePool);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create cluster", e);
        }
    }
}
