package com.hartwig.platinum.kubernetes;

import static java.util.stream.Collectors.toMap;

import java.util.Map.Entry;

import com.hartwig.platinum.config.PlatinumConfiguration;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

public class PipelineConfigMapVolume {
    private final PlatinumConfiguration configuration;
    private final KubernetesClient kubernetesClient;

    public PipelineConfigMapVolume(PlatinumConfiguration configuration, KubernetesClient kubernetesClient) {
        this.configuration = configuration;
        this.kubernetesClient = kubernetesClient;
    }

    public Volume create(final String name) {
        kubernetesClient.configMaps()
                .inNamespace(KubernetesCluster.NAMESPACE)
                .withName(name)
                .createOrReplaceWithNew()
                .addToData(configuration.samples().entrySet().stream().collect(toMap(Entry::getKey, e -> e.getValue().toString())))
                .withNewMetadata()
                .withName(name)
                .withNamespace(KubernetesCluster.NAMESPACE)
                .endMetadata()
                .done();
        return new VolumeBuilder().withName(name)
                .editOrNewConfigMap()
                .withName(name)
                .endConfigMap()
                .build();
    }
}
