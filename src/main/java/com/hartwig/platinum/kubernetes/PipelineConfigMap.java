package com.hartwig.platinum.kubernetes;

import static java.util.stream.Collectors.toMap;

import java.util.Map.Entry;

import com.hartwig.platinum.config.PlatinumConfiguration;

import io.fabric8.kubernetes.client.KubernetesClient;

public class PipelineConfigMap {
    private final PlatinumConfiguration configuration;
    private final KubernetesClient kubernetesClient;

    public PipelineConfigMap(PlatinumConfiguration configuration, KubernetesClient kubernetesClient) {
        this.configuration = configuration;
        this.kubernetesClient = kubernetesClient;
    }

    public String create() {
        String name = "samples";
        kubernetesClient.configMaps()
                .createNew()
                .addToData(configuration.samples().entrySet().stream().collect(toMap(Entry::getKey, e -> e.getValue().toString())))
                .withNewMetadata()
                .withName(name)
                .withNamespace(KubernetesCluster.NAMESPACE)
                .endMetadata()
                .done();
        return name;
    }
}
