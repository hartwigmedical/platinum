package com.hartwig.platinum.kubernetes;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static java.util.List.of;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.container.v1beta1.Container;
import com.google.api.services.container.v1beta1.Container.Projects.Locations.Clusters.Create;
import com.google.api.services.container.v1beta1.Container.Projects.Locations.Clusters.Get;
import com.google.api.services.container.v1beta1.model.Cluster;
import com.google.api.services.container.v1beta1.model.CreateClusterRequest;
import com.google.api.services.container.v1beta1.model.IPAllocationPolicy;
import com.google.api.services.container.v1beta1.model.NodeConfig;
import com.google.api.services.container.v1beta1.model.NodePool;
import com.google.api.services.container.v1beta1.model.Operation;
import com.google.api.services.container.v1beta1.model.PrivateClusterConfig;
import com.hartwig.platinum.Console;
import com.hartwig.platinum.config.GcpConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.iam.JsonKey;
import com.hartwig.platinum.p5sample.TumorNormalPair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

public class KubernetesEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesEngine.class);
    private final Container containerApi;
    private final ProcessRunner processRunner;
    private final PlatinumConfiguration configuration;

    public KubernetesEngine(final Container containerApi, final ProcessRunner processRunner, final PlatinumConfiguration configuration) {
        this.containerApi = containerApi;
        this.processRunner = processRunner;
        this.configuration = configuration;
    }

    private Optional<Cluster> find(final String path) throws IOException {
        try {
            Get found = containerApi.projects().locations().clusters().get(path);
            return Optional.of(found.execute());
        } catch (GoogleJsonResponseException e) {
            return Optional.empty();
        }
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
            if (!gcpConfiguration.networkTags().isEmpty()) {
                defaultNodePool.setConfig(new NodeConfig().setTags(gcpConfiguration.networkTags())
                        .setPreemptible(gcpConfiguration.preemptibleCluster())
                        .setDiskSizeGb(500));
            }
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

    public KubernetesCluster findOrCreate(final String runName, final List<TumorNormalPair> pairs, final JsonKey jsonKey,
            final String outputBucketName, final String serviceAccountEmail) {
        try {
            String clusterName = configuration.cluster().orElse(runName);
            GcpConfiguration gcpConfiguration = configuration.gcp();
            String parent = String.format("projects/%s/locations/%s", gcpConfiguration.projectOrThrow(), gcpConfiguration.regionOrThrow());
            if (find(fullPath(gcpConfiguration.projectOrThrow(), gcpConfiguration.regionOrThrow(), clusterName)).isEmpty()) {
                create(containerApi, parent, clusterName, gcpConfiguration);
            }
            if (!processRunner.execute(of("gcloud",
                    "container",
                    "clusters",
                    "get-credentials",
                    clusterName,
                    "--region",
                    gcpConfiguration.regionOrThrow(),
                    "--project",
                    gcpConfiguration.projectOrThrow()))) {
                throw new RuntimeException("Failed to get credentials for cluster");
            }
            if (!processRunner.execute(of("kubectl", "get", "configmaps"))) {
                throw new RuntimeException("Failed to run kubectl command against cluster");
            }
            LOGGER.info("Connection to cluster {} configured via gcloud and kubectl", Console.bold(clusterName));
            DefaultKubernetesClient kubernetesClient = new DefaultKubernetesClient();

            TargetNodePool targetNodePool = configuration.gcp()
                    .nodePoolConfiguration()
                    .map(c -> TargetNodePool.fromConfig(c,
                            configuration.samples().isEmpty() ? configuration.sampleIds().size() : configuration.samples().size()))
                    .orElse(TargetNodePool.defaultPool());
            if (!targetNodePool.isDefault()) {
                new GcloudNodePool(processRunner).create(targetNodePool,
                        serviceAccountEmail,
                        clusterName,
                        gcpConfiguration.projectOrThrow());
            }
            return new KubernetesCluster(runName,
                    new JobScheduler(kubernetesClient),
                    new PipelineServiceAccountSecretVolume(jsonKey, kubernetesClient, "service-account-key"),
                    new PipelineConfigMapVolume(pairs, kubernetesClient, runName),
                    outputBucketName,
                    serviceAccountEmail,
                    configuration,
                    Delay.threadSleep(),
                    targetNodePool);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create cluster", e);
        }
    }
}
