package com.hartwig.platinum.kubernetes;

import static java.lang.String.format;

import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

public class PipelineConfigMapVolume implements KubernetesComponent<Volume> {
    private final KubernetesClient kubernetesClient;
    private final String volumeName;
    private final String sample;
    private final String content;

    private PipelineConfigMapVolume(final KubernetesClient kubernetesClient, final String runName, final String sample, final String content) {
        this.kubernetesClient = kubernetesClient;
        this.volumeName = format("%s-%s", runName, sample);
        this.sample = sample;
        this.content = content;
    }

    @Override
    public Volume asKubernetes() {
        kubernetesClient.configMaps()
                .inNamespace(KubernetesCluster.NAMESPACE)
                .withName(volumeName)
                .createOrReplace(new ConfigMapBuilder().addToData(Map.of(sample, content))
                        .withNewMetadata()
                        .withName(volumeName)
                        .withNamespace(KubernetesCluster.NAMESPACE)
                        .endMetadata()
                        .build());
        return new VolumeBuilder().withName(volumeName).editOrNewConfigMap().withName(volumeName).endConfigMap().build();
    }

    public static class PipelineConfigMapVolumeBuilder {
        private final KubernetesClient kubernetesClient;

        public PipelineConfigMapVolumeBuilder(final KubernetesClient kubernetesClient) {
            this.kubernetesClient = kubernetesClient;
        }

        public PipelineConfigMapVolume of(String runName, String sample, String content) {
            return new PipelineConfigMapVolume(kubernetesClient, runName, sample, content);
        }
    }
}
