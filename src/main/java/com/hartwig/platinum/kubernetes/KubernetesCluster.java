package com.hartwig.platinum.kubernetes;

import java.util.ArrayList;
import java.util.List;

import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.iam.JsonKey;

import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.client.KubernetesClient;

public class KubernetesCluster {

    private static final String CONFIG_MAP_NAME = "sample";
    private static final String SERVICE_ACCOUNT_KEY_NAME = "service-account-key";
    private final String runName;
    private final String outputBucket;
    private final KubernetesClient kubernetesClient;

    final static String NAMESPACE = "default";

    private KubernetesCluster(final String runName, final String outputBucket, final KubernetesClient kubernetesClient) {
        this.runName = runName.toLowerCase();
        this.outputBucket = outputBucket;
        this.kubernetesClient = kubernetesClient;
    }

    public List<Job> submit(final PlatinumConfiguration configuration, final JsonKey jsonKey) {
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
                                    outputBucket).create(CONFIG_MAP_NAME, SERVICE_ACCOUNT_KEY_NAME),
                            new PipelineConfigMapVolume(configuration, kubernetesClient).create(CONFIG_MAP_NAME),
                            new PipelineServiceAccountSecretVolume(jsonKey, kubernetesClient).create(SERVICE_ACCOUNT_KEY_NAME)))
                    .done());
        }
        return jobs;
    }

    public static KubernetesCluster findOrCreate(final String runName, final String outputBucket, final KubernetesClient kubernetesClient) {
        try {
            return new KubernetesCluster(runName, outputBucket, kubernetesClient);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create cluster", e);
        }
    }
}
