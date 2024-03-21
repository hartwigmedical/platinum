package com.hartwig.platinum.kubernetes.pipeline;

import static java.lang.String.format;

import static com.hartwig.platinum.kubernetes.KubernetesUtil.toValidRFC1123Label;

import java.util.Map;

import com.hartwig.platinum.kubernetes.KubernetesClientProxy;
import com.hartwig.platinum.kubernetes.KubernetesComponent;

import io.fabric8.kubernetes.api.model.Volume;

public class PipelineConfigMapVolume implements KubernetesComponent<Volume> {
    private final KubernetesClientProxy kubernetesClientProxy;
    private final String volumeName;
    private final String sample;
    private final String content;

    private PipelineConfigMapVolume(final KubernetesClientProxy kubernetesClientProxy, final String runName, final String sample,
            final String content) {
        this.kubernetesClientProxy = kubernetesClientProxy;
        this.volumeName = toValidRFC1123Label(format("%s-%s", runName, sample));
        this.sample = sample;
        this.content = content;
    }

    @Override
    public Volume asKubernetes() {
        return kubernetesClientProxy.ensureConfigMapVolumeExists(volumeName, Map.of(sample, content));
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
