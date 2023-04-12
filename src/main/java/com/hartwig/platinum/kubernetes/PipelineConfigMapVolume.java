package com.hartwig.platinum.kubernetes;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

public class PipelineConfigMapVolume implements KubernetesComponent<Volume> {

    private static final String SAMPLES = "samples";
    private final List<TumorNormalPair> pairs;
    private final KubernetesClient kubernetesClient;
    private final String runName;

    public PipelineConfigMapVolume(final List<TumorNormalPair> pairs, final KubernetesClient kubernetesClient, final String runName) {
        this.kubernetesClient = kubernetesClient;
        this.runName = runName;
        this.pairs = pairs;
    }

    public Volume asKubernetes() {
        String name = runName + "-" + SAMPLES;
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        kubernetesClient.configMaps()
                .inNamespace(KubernetesCluster.NAMESPACE)
                .withName(name)
                .createOrReplace(new ConfigMapBuilder().addToData(pairs.stream()
                        .collect(Collectors.toMap(p -> p.name().toLowerCase() + "-" + runName, p -> toJson(objectMapper, p))))
                        .withNewMetadata()
                        .withName(name)
                        .withNamespace(KubernetesCluster.NAMESPACE)
                        .endMetadata()
                        .build());
        return new VolumeBuilder().withName(name).editOrNewConfigMap().withName(name).endConfigMap().build();
    }

    private String toJson(final ObjectMapper objectMapper, final TumorNormalPair tumorNormalPair) {
        try {
            return objectMapper.writeValueAsString(tumorNormalPair);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
