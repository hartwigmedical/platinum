package com.hartwig.platinum.kubernetes.pipeline;

import static java.lang.String.format;

import java.util.Map;

import com.hartwig.platinum.kubernetes.KubernetesClientProxy;
import com.hartwig.platinum.kubernetes.KubernetesCluster;
import com.hartwig.platinum.kubernetes.KubernetesComponent;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;

public class PipelineConfigMapVolume implements KubernetesComponent<Volume> {
    private final KubernetesClientProxy kubernetesClientProxy;
    private final String volumeName;
    private final String sample;
    private final String content;

    private PipelineConfigMapVolume(final KubernetesClientProxy kubernetesClientProxy, final String runName, final String sample, final String content) {
        this.kubernetesClientProxy = kubernetesClientProxy;
        this.volumeName = format("%s-%s", runName, sample);
        this.sample = sample;
        this.content = content;
    }

    @Override
    public Volume asKubernetes() {
        try {
            kubernetesClientProxy.configMaps()
                    .inNamespace(KubernetesCluster.NAMESPACE)
                    .withName(volumeName)
                    .createOrReplace(new ConfigMapBuilder().addToData(Map.of(sample, content))
                            .withNewMetadata()
                            .withName(volumeName)
                            .withNamespace(KubernetesCluster.NAMESPACE)
                            .endMetadata()
                            .build());
            return new VolumeBuilder().withName(volumeName).editOrNewConfigMap().withName(volumeName).endConfigMap().build();
        } catch (KubernetesClientException e) {
            kubernetesClientProxy.reAuthorise();
            return asKubernetes();
        }
    }

    public static class PipelineConfigMapVolumeBuilder {
        private final KubernetesClientProxy kubernetesClient;

        public PipelineConfigMapVolumeBuilder(final KubernetesClientProxy kubernetesClient) {
            this.kubernetesClient = kubernetesClient;
        }

        public PipelineConfigMapVolume of(String runName, String sample, String content) {
            return new PipelineConfigMapVolume(kubernetesClient, runName, sample, content);
        }
    }
}
