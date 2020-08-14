package com.hartwig.platinum.kubernetes;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.singleton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.container.v1beta1.Container;
import com.google.api.services.container.v1beta1.Container.Projects.Locations.Clusters.Create;
import com.google.api.services.container.v1beta1.Container.Projects.Locations.Clusters.Get;
import com.google.api.services.container.v1beta1.ContainerScopes;
import com.google.api.services.container.v1beta1.model.ClientCertificateConfig;
import com.google.api.services.container.v1beta1.model.Cluster;
import com.google.api.services.container.v1beta1.model.CreateClusterRequest;
import com.google.api.services.container.v1beta1.model.MasterAuth;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.iam.JsonKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

public class KubernetesCluster {

    private static final String CONFIG_MAP_NAME = "sample";
    private static final String SERVICE_ACCOUNT_KEY_NAME = "service-account-key";
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesCluster.class);
    private final String runName;
    private final KubernetesClient kubernetesClient;

    final static String NAMESPACE = "default";

    private KubernetesCluster(final String runName, final KubernetesClient kubernetesClient) {
        this.runName = runName.toLowerCase();
        this.kubernetesClient = kubernetesClient;
    }

    public List<Job> submit(final PlatinumConfiguration configuration, final JsonKey jsonKey, final String outputBucketName) {
        Volume configMapVolume = new PipelineConfigMapVolume(configuration, kubernetesClient).create(CONFIG_MAP_NAME);
        Volume secretVolume = new PipelineServiceAccountSecretVolume(jsonKey, kubernetesClient).create(SERVICE_ACCOUNT_KEY_NAME);

        List<Job> jobs = new ArrayList<>();

        for (String sample : configuration.samples().keySet()) {
            String jobName = String.format("platinum-%s-%s", this.runName, sample).toLowerCase();
            jobs.add(kubernetesClient.batch()
                    .jobs()
                    .createNew()
                    .withNewMetadata()
                    .withName(jobName)
                    .withNamespace(NAMESPACE)
                    .endMetadata()
                    .withSpec(new PipelineJob().create(new PipelineContainer(sample,
                                    runName,
                                    configuration.pipelineArguments(),
                                    outputBucketName).create(CONFIG_MAP_NAME, SERVICE_ACCOUNT_KEY_NAME),
                            configMapVolume, secretVolume))
                    .done());
        }
        return jobs;
    }

    private static String fullPath(String runName) {
        return "projects/hmf-pipeline-development/locations/europe-west4/clusters/" + runName + "-cluster";
    }

    private static Optional<Cluster> find(final Container containerApi, final String path) throws IOException {
        try {
            Get found = containerApi.projects().locations().clusters().get(path);
            return Optional.of(found.execute());
        } catch (GoogleJsonResponseException e) {
            LOGGER.info("No cluster found at [{}]", path);
            return Optional.empty();
        }
    }

    private static void create(final Container containerApi, final String parent, final String runName) {
        try {
            String clusterName = runName + "-cluster";
            Cluster newCluster = new Cluster();
            newCluster.setName(clusterName);
            newCluster.setInitialNodeCount(3);
            ClientCertificateConfig certificateConfig = new ClientCertificateConfig();
            certificateConfig.setIssueClientCertificate(true);
            newCluster.setMasterAuth(new MasterAuth());
            newCluster.getMasterAuth().setClientCertificateConfig(certificateConfig);
            CreateClusterRequest createRequest = new CreateClusterRequest();
            createRequest.setCluster(newCluster);
            Create created = containerApi.projects().locations().clusters().create(parent, createRequest);
            created.execute();
            LOGGER.info("Cluster creation started");
            boolean found = Failsafe.with(
                    new RetryPolicy<>().withMaxDuration(ofMinutes(15)).withDelay(ofSeconds(15)).withMaxAttempts(-1).handleResult(null).handleResult(Boolean.FALSE)).get(() -> {
                try {
                    return "RUNNING".equals(containerApi.projects().locations().clusters().get(fullPath(runName)).execute().getStatus());
                } catch (Exception e) {
                    LOGGER.error("Exception While waiting for cluster create", e);
                    return null;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to create cluster", e);
        }
    }

    public static KubernetesCluster findOrCreate(final String runName) {
        try {
            Container container = new Container.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(GoogleCredentials.getApplicationDefault().createScoped(singleton(ContainerScopes.CLOUD_PLATFORM)))).build();

            String clusterName = runName + "-cluster";
            String parent = "projects/hmf-pipeline-development/locations/europe-west4";
            if (find(container, fullPath(runName)).isEmpty()) {
                create(container, parent, runName);
            }
            ProcessBuilder processBuilder =
                    new ProcessBuilder("/usr/bin/gcloud", "container", "clusters", "get-credentials", clusterName, "--zone", "europe-west4");
            Process process = processBuilder.inheritIO().start();
            process.waitFor();
            processBuilder = new ProcessBuilder("/usr/bin/kubectl", "get", "pods");
            process = processBuilder.inheritIO().start();
            process.waitFor();

            return new KubernetesCluster(runName, new DefaultKubernetesClient());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create cluster", e);
        }
    }
}
