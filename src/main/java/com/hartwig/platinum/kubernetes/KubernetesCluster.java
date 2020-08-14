package com.hartwig.platinum.kubernetes;

import static java.util.Collections.singleton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import com.google.api.services.container.v1beta1.model.Operation;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.iam.JsonKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

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
//        String configMapName = new PipelineConfigMapVolume(configuration, kubernetesClient).create();
//        String keySecretName = new PipelineServiceAccountSecretVolume(keyJson, kubernetesClient).create();
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
                            new PipelineConfigMapVolume(configuration, kubernetesClient).create(CONFIG_MAP_NAME),
                            new PipelineServiceAccountSecretVolume(jsonKey, kubernetesClient).create(SERVICE_ACCOUNT_KEY_NAME)))
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

    private static Cluster create(final Container containerApi, final String parent, final String runName) {
        try {
            Cluster newCluster = new Cluster();
            newCluster.setName(runName + "-cluster");
            newCluster.setInitialNodeCount(3);
            ClientCertificateConfig certificateConfig = new ClientCertificateConfig();
            certificateConfig.setIssueClientCertificate(true);
            newCluster.setMasterAuth(new MasterAuth());
            newCluster.getMasterAuth().setClientCertificateConfig(certificateConfig);
            CreateClusterRequest createRequest = new CreateClusterRequest();
            createRequest.setCluster(newCluster);
            Create created = containerApi.projects().locations().clusters().create(parent, createRequest);
            Operation execute = created.execute();
            return find(containerApi, fullPath(runName)).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create cluster", e);
        }
    }

    public static KubernetesCluster findOrCreate(final String runName) {
        try {
            Container container = new Container.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(GoogleCredentials.getApplicationDefault().createScoped(singleton(ContainerScopes.CLOUD_PLATFORM)))).build();

            String parent = "projects/hmf-pipeline-development/locations/europe-west4";
            Cluster cluster = find(container, fullPath(runName)).orElseGet(() -> create(container, parent, runName));

            final io.fabric8.kubernetes.client.Config kubeConfig = new ConfigBuilder().withMasterUrl("https://" + cluster.getEndpoint())
                    .withCaCertData(cluster.getMasterAuth().getClusterCaCertificate())
                    .withClientCertData(cluster.getMasterAuth().getClientCertificate())
                    .withClientKeyData(cluster.getMasterAuth().getClientKey())
                    .withNamespace(KubernetesCluster.NAMESPACE)
                    .withRequestTimeout(60_000)
                    .withConnectionTimeout(60_000)
                    .build();

            final OkHttpClient httpClient = HttpClientUtils.createHttpClient(kubeConfig).newBuilder()
                    .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                    .build();
            return new KubernetesCluster(runName, new DefaultKubernetesClient(httpClient, kubeConfig));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create cluster", e);
        }
    }
}
