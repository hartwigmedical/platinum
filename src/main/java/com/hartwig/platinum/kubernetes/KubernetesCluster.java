package com.hartwig.platinum.kubernetes;

import java.util.ArrayList;
import java.util.List;

import com.hartwig.platinum.GcpConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.iam.JsonKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.client.KubernetesClient;

public class KubernetesCluster {

    private static final String CONFIG_MAP_NAME = "sample";
    private static final String SERVICE_ACCOUNT_KEY_NAME = "service-account-key";
    private final String runName;
    private final KubernetesClient kubernetesClient;

    final static String NAMESPACE = "default";

    KubernetesCluster(final String runName, final KubernetesClient kubernetesClient) {
        this.runName = runName.toLowerCase();
        this.kubernetesClient = kubernetesClient;
    }

    public List<Job> submit(final PlatinumConfiguration configuration, final JsonKey jsonKey, final String outputBucketName,
            final GcpConfiguration gcpConfiguration, final String serviceAccountEmail) {
        Volume configMapVolume = new PipelineConfigMapVolume(configuration, kubernetesClient).create(CONFIG_MAP_NAME);
        Volume secretVolume = new PipelineServiceAccountSecretVolume(jsonKey, kubernetesClient).create(SERVICE_ACCOUNT_KEY_NAME);

        List<Job> jobs = new ArrayList<>();

        for (String sample : configuration.samples().keySet()) {
            jobs.add(kubernetesClient.batch()
                    .jobs()
                    .createNew()
                    .withNewMetadata()
                    .withName(sample.toLowerCase())
                    .withNamespace(NAMESPACE)
                    .endMetadata()
                    .withSpec(new PipelineJob().create(new PipelineContainer(sample,
                            runName,
                            new PipelineArguments(configuration.argumentOverrides(),
                                    outputBucketName,
                                    serviceAccountEmail,
                                    sample,
                                    runName,
                                    gcpConfiguration)).create(CONFIG_MAP_NAME, SERVICE_ACCOUNT_KEY_NAME), configMapVolume, secretVolume))
                    .done());
        }
        return jobs;
    }
}
