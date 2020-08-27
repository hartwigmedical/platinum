package com.hartwig.platinum.kubernetes;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import java.io.IOException;
import java.util.Optional;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.container.v1beta1.Container;
import com.google.api.services.container.v1beta1.Container.Projects.Locations.Clusters.Create;
import com.google.api.services.container.v1beta1.Container.Projects.Locations.Clusters.Get;
import com.google.api.services.container.v1beta1.model.Cluster;
import com.google.api.services.container.v1beta1.model.CreateClusterRequest;
import com.google.api.services.container.v1beta1.model.Operation;
import com.hartwig.platinum.Console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

public class KubernetesClusterProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesClusterProvider.class);
    private Container containerApi;

    public KubernetesClusterProvider(final Container containerApi) {
        this.containerApi = containerApi;
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

    private static void create(final Container containerApi, final String parent, final String project, final String region,
            final String runName) {
        try {
            String clusterName = runName + "-cluster";
            Cluster newCluster = new Cluster();
            newCluster.setName(clusterName);
            newCluster.setInitialNodeCount(1);
            CreateClusterRequest createRequest = new CreateClusterRequest();
            createRequest.setCluster(newCluster);
            Create created = containerApi.projects().locations().clusters().create(parent, createRequest);
            Operation execute = created.execute();
            LOGGER.info("Creating new kubernetes cluster {} in project {} and region {}, this can take upwards of 5 minutes...",
                    Console.bold(newCluster.getName()),
                    Console.bold(project),
                    Console.bold(region));
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
                                .get(String.format("projects/%s/locations/%s/operations/%s", project, region, execute.getName()))
                                .execute()
                                .getStatus());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create cluster", e);
        }
    }


    public KubernetesCluster findOrCreate(final String runName, final String project, final String region) {
        try {
            String clusterName = runName + "-cluster";
            String parent = String.format("projects/%s/locations/%s", project, region);
            if (find(fullPath(project, region, runName)).isEmpty()) {
                create(containerApi, parent, project, region, runName);
            }
            ProcessBuilder processBuilder = new ProcessBuilder("gcloud",
                    "container",
                    "clusters",
                    "get-credentials",
                    clusterName,
                    "--region",
                    region,
                    "--project",
                    project);
            Process process = processBuilder.start();
            process.waitFor();
            processBuilder = new ProcessBuilder("kubectl", "get", "pods");
            process = processBuilder.start();
            process.waitFor();
            LOGGER.info("Connection to cluster {} configured via gcloud and kubectl", Console.bold(clusterName));
            return new KubernetesCluster(runName, new DefaultKubernetesClient());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create cluster", e);
        }
    }
}
