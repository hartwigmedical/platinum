package com.hartwig.platinum.kubernetes;

import static java.util.stream.Collectors.toMap;

import java.util.Map.Entry;

import com.hartwig.platinum.config.PlatinumConfiguration;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

public class PipelineConfigMapVolume implements KubernetesComponent<Volume> {

    private static final String SAMPLE = "sample";
    private final PlatinumConfiguration configuration;
    private final KubernetesClient kubernetesClient;

    public PipelineConfigMapVolume(final PlatinumConfiguration configuration, final KubernetesClient kubernetesClient) {
        this.configuration = configuration;
        this.kubernetesClient = kubernetesClient;
    }

    public Volume asKubernetes() {
        kubernetesClient.configMaps()
                .inNamespace(KubernetesCluster.NAMESPACE)
                .withName(SAMPLE)
                .createOrReplaceWithNew()
                .addToData(configuration.samples().entrySet().stream().collect(toMap(Entry::getKey, e -> e.getValue().toString())))
                .withNewMetadata()
                .withName(SAMPLE)
                .withNamespace(KubernetesCluster.NAMESPACE)
                .endMetadata()
                .done();
        return new VolumeBuilder().withName(SAMPLE).editOrNewConfigMap().withName(SAMPLE).endConfigMap().build();
    }
}
