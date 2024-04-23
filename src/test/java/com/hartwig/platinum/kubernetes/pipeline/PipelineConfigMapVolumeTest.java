package com.hartwig.platinum.kubernetes.pipeline;

import static java.lang.String.format;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;

import com.hartwig.platinum.kubernetes.KubernetesClientProxy;
import com.hartwig.platinum.kubernetes.pipeline.PipelineConfigMapVolume.PipelineConfigMapVolumeBuilder;

import org.junit.Test;

public class PipelineConfigMapVolumeTest {
    @Test
    @SuppressWarnings("unchecked")
    public void shouldBuildVolume() {
        String runName = "run-name";
        String sample = "sample";
        String content = "content";
        KubernetesClientProxy kubernetesClient = mock(KubernetesClientProxy.class);
        new PipelineConfigMapVolumeBuilder(kubernetesClient).of(runName, sample, content).asKubernetes();

        verify(kubernetesClient).ensureConfigMapVolumeExists(format("%s-%s", runName, sample), Map.of(sample, content));
    }
}