package com.hartwig.platinum.kubernetes.pipeline;

import static java.lang.String.format;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hartwig.platinum.kubernetes.KubernetesClientProxy;
import com.hartwig.platinum.kubernetes.KubernetesCluster;
import com.hartwig.platinum.kubernetes.pipeline.PipelineConfigMapVolume.PipelineConfigMapVolumeBuilder;

import org.junit.Test;
import org.mockito.Mockito;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeFluent;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class PipelineConfigMapVolumeTest {
    @Test
    @SuppressWarnings("unchecked")
    public void shouldBuildVolume() {
        KubernetesClientProxy kubernetesClient = mock(KubernetesClientProxy.class);
        MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> configMaps = Mockito.mock(MixedOperation.class);
        NonNamespaceOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> namespaceable = Mockito.mock(NonNamespaceOperation.class);
        Resource<ConfigMap> nameable = Mockito.mock(Resource.class);
        when(kubernetesClient.configMaps()).thenReturn(configMaps);

        when(configMaps.inNamespace(KubernetesCluster.NAMESPACE)).thenReturn(namespaceable);
        String runName = "run-name";
        String sample = "sample";
        String volumeName = format("%s-%s", runName, sample);
        when(namespaceable.withName(volumeName)).thenReturn(nameable);
        String content = "content";

        ConfigMapBuilder mapBuilder = mock(ConfigMapBuilder.class);
        ConfigMap configMap = mock(ConfigMap.class);
        when(kubernetesClient.newConfigMapBuilder()).thenReturn(mapBuilder);
        when(mapBuilder.addToData(any())).thenReturn(mapBuilder);
        io.fabric8.kubernetes.api.model.ConfigMapFluent.MetadataNested<ConfigMapBuilder> metadataNested = mock(io.fabric8.kubernetes.api.model.ConfigMapFluent.MetadataNested.class);
        when(mapBuilder.withNewMetadata()).thenReturn(metadataNested);
        when(metadataNested.withName(any())).thenReturn(metadataNested);
        when(metadataNested.withNamespace(any())).thenReturn(metadataNested);
        when(metadataNested.endMetadata()).thenReturn(mapBuilder);
        when(mapBuilder.build()).thenReturn(configMap);

        VolumeBuilder volumeBuilder = mock(VolumeBuilder.class);
        VolumeFluent.ConfigMapNested<VolumeBuilder> volumeBuilderConfigMap = mock(VolumeFluent.ConfigMapNested.class);
        when(kubernetesClient.newVolumeBuilder()).thenReturn(volumeBuilder);
        when(volumeBuilder.withName(any())).thenReturn(volumeBuilder);
        when(volumeBuilder.editOrNewConfigMap()).thenReturn(volumeBuilderConfigMap);
        when(volumeBuilderConfigMap.withName(any())).thenReturn(volumeBuilderConfigMap);
        when(volumeBuilderConfigMap.endConfigMap()).thenReturn(volumeBuilder);
        when(volumeBuilder.build()).thenReturn(mock(Volume.class));

        new PipelineConfigMapVolumeBuilder(kubernetesClient).of(runName, sample, content).asKubernetes();

        verify(metadataNested).withName(volumeName);
        verify(metadataNested).withNamespace("default");
        verify(volumeBuilder).withName(volumeName);
    }
}