package com.hartwig.platinum.kubernetes;

import static java.lang.String.format;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hartwig.platinum.kubernetes.PipelineConfigMapVolume.PipelineConfigMapVolumeBuilder;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class PipelineConfigMapVolumeTest {
    @Test
    public void shouldBuildVolume() {
        KubernetesClient kubernetesClient = mock(KubernetesClient.class);
        MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> configMaps = Mockito.<MixedOperation>mock(MixedOperation.class);
        NonNamespaceOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> namespaceable = Mockito.<NonNamespaceOperation>mock(NonNamespaceOperation.class);
        Resource<ConfigMap> nameable = Mockito.<Resource>mock(Resource.class);
        when(kubernetesClient.configMaps()).thenReturn(configMaps);
        when(configMaps.inNamespace(KubernetesCluster.NAMESPACE)).thenReturn(namespaceable);
        String runName = "run-name";
        String sample = "sample";
        String volumeName = format("%s-%s", runName, sample);
        when(namespaceable.withName(volumeName)).thenReturn(nameable);
        String content = "content";

        Volume volume = new PipelineConfigMapVolumeBuilder(kubernetesClient).of(runName, sample, content).asKubernetes();

        ArgumentCaptor<ConfigMap> configMapCaptor = ArgumentCaptor.forClass(ConfigMap.class);
        verify(nameable).createOrReplace(configMapCaptor.capture());
        ConfigMap actual = configMapCaptor.getValue();
        assertThat(actual.getData().keySet().size()).isEqualTo(1);
        assertThat(actual.getData()).containsKey(sample);
        assertThat(actual.getData().get(sample)).isEqualTo(content);
        assertThat(actual.getMetadata().getName()).isEqualTo(volumeName);
        assertThat(actual.getMetadata().getNamespace()).isEqualTo(KubernetesCluster.NAMESPACE);

        assertThat(volume.getName()).isEqualTo(volumeName);
    }
}