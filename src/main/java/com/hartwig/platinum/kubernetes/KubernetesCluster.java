package com.hartwig.platinum.kubernetes;

import java.util.List;
import java.util.stream.Collectors;

import com.hartwig.platinum.config.GcpConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

public class KubernetesCluster {

    private final String runName;
    private final KubernetesClient kubernetesClient;

    final static String NAMESPACE = "default";
    private final PipelineServiceAccountSecretVolume secret;
    private final String outputBucketName;
    private final GcpConfiguration gcpConfiguration;
    private final String serviceAccountEmail;

    KubernetesCluster(final String runName, final KubernetesClient kubernetesClient, final PipelineServiceAccountSecretVolume secret,
            final String outputBucketName, final GcpConfiguration gcpConfiguration, final String serviceAccountEmail) {
        this.runName = runName.toLowerCase();
        this.kubernetesClient = kubernetesClient;
        this.secret = secret;
        this.outputBucketName = outputBucketName;
        this.gcpConfiguration = gcpConfiguration;
        this.serviceAccountEmail = serviceAccountEmail;
    }

    public void submit(final PlatinumConfiguration configuration) {
        Volume configMapVolume = new PipelineConfigMapVolume(configuration, kubernetesClient, "sample").asKubernetes();
        Volume secretVolume = this.secret.asKubernetes();
        Volume jksVolume = new VolumeBuilder().withName("jks").editOrNewSecret().withSecretName("jks").endSecret().build();
        for (SampleArgument sample : samples(configuration)) {
            kubernetesClient.batch()
                    .jobs()
                    .createNew()
                    .withNewMetadata()
                    .withName(sample.id())
                    .withNamespace(NAMESPACE)
                    .endMetadata()
                    .withSpec(new PipelineJob(new PipelineContainer(sample,
                            runName,
                            new PipelineArguments(configuration.argumentOverrides(),
                                    outputBucketName,
                                    serviceAccountEmail,
                                    runName,
                                    configuration),
                            secretVolume.getName(),
                            configMapVolume.getName(),
                            configuration.image()).asKubernetes(), configMapVolume, secretVolume, jksVolume).asKubernetes())
                    .done();
        }
    }

    public List<SampleArgument> samples(final PlatinumConfiguration configuration) {
        return configuration.biopsies().isEmpty() ? configuration.samples()
                .keySet()
                .stream()
                .map(SampleArgument::sampleJson)
                .collect(Collectors.toList()) : configuration.biopsies().stream().map(SampleArgument::biopsy).collect(Collectors.toList());
    }
}
