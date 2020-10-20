package com.hartwig.platinum.kubernetes;

import static java.lang.String.format;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static java.util.List.of;

import java.io.IOException;
import java.util.Optional;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.container.v1beta1.Container;
import com.google.api.services.container.v1beta1.Container.Projects.Locations.Clusters.Create;
import com.google.api.services.container.v1beta1.Container.Projects.Locations.Clusters.Get;
import com.google.api.services.container.v1beta1.model.Cluster;
import com.google.api.services.container.v1beta1.model.CreateClusterRequest;
import com.google.api.services.container.v1beta1.model.NodeConfig;
import com.google.api.services.container.v1beta1.model.Operation;
import com.google.api.services.container.v1beta1.model.PrivateClusterConfig;
import com.hartwig.platinum.Console;
import com.hartwig.platinum.GcpConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

public class KubernetesEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesEngine.class);
    private final Container containerApi;
    private final ProcessRunner processRunner;

    public KubernetesEngine(final Container containerApi, final ProcessRunner processRunner) {
        this.containerApi = containerApi;
        this.processRunner = processRunner;
    }

    private Optional<Cluster> find(final String path) throws IOException {
        try {
            Get found = containerApi.projects().locations().clusters().get(path);
            return Optional.of(found.execute());
        } catch (GoogleJsonResponseException e) {
            return Optional.empty();
        }
    }

    private static String fullPath(final String project, final String region, final String runName) {
        return String.format("projects/%s/locations/%s/clusters/%s-cluster", project, region, runName);
    }

    private static void create(final Container containerApi, final String parent, final GcpConfiguration gcpConfiguration,
            final String runName) {
        try {
            String clusterName = runName + "-cluster";
            Cluster newCluster = new Cluster();
            newCluster.setName(clusterName);
            newCluster.setInitialNodeCount(1);
            newCluster.setNetwork(gcpConfiguration.networkUrl());
            newCluster.setSubnetwork(gcpConfiguration.subnetUrl());
            NodeConfig nodeConfig = new NodeConfig();
            nodeConfig.setTags(gcpConfiguration.networkTags());
            newCluster.setNodeConfig(nodeConfig);
            CreateClusterRequest createRequest = new CreateClusterRequest();
            createRequest.setCluster(newCluster);
            Create created = containerApi.projects().locations().clusters().create(parent, createRequest);
            Operation execute = created.execute();
            LOGGER.info("Creating new kubernetes cluster {} in project {} and region {}, this can take upwards of 5 minutes...",
                    Console.bold(newCluster.getName()),
                    Console.bold(gcpConfiguration.project()),
                    Console.bold(gcpConfiguration.region()));
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
                                    gcpConfiguration.project(),
                                    gcpConfiguration.region(),
                                    execute.getName()))
                            .execute()
                            .getStatus());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create cluster", e);
        }
    }

    private static boolean isUrl(final GcpConfiguration gcpConfiguration) {
        return gcpConfiguration.network().startsWith("projects");
    }

    public KubernetesCluster findOrCreate(final String runName, final GcpConfiguration gcpConfiguration) {
        try {
            String clusterName = runName + "-cluster";
            String parent = String.format("projects/%s/locations/%s", gcpConfiguration.project(), gcpConfiguration.region());
            if (find(fullPath(gcpConfiguration.project(), gcpConfiguration.region(), runName)).isEmpty()) {
                create(containerApi, parent, gcpConfiguration, runName);
            }
            if (!processRunner.execute(of("gcloud",
                    "container",
                    "clusters",
                    "get-credentials",
                    clusterName,
                    "--region",
                    gcpConfiguration.region(),
                    "--project",
                    gcpConfiguration.project()))) {
                throw new RuntimeException("Failed to get credentials for cluster");
            }
            if (!processRunner.execute(of("kubectl", "get", "pods"))) {
                throw new RuntimeException("Failed to run kubectl command against cluster");
            }
            LOGGER.info("Connection to cluster {} configured via gcloud and kubectl", Console.bold(clusterName));
            return new KubernetesCluster(runName, new DefaultKubernetesClient());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create cluster", e);
        }
    }
}