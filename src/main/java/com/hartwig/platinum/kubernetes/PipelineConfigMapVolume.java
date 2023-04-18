package com.hartwig.platinum.kubernetes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.hartwig.pdl.PipelineInput;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

public class PipelineConfigMapVolume implements KubernetesComponent<Volume> {

    private static final String SAMPLES = "samples";
    private final KubernetesClient kubernetesClient;
    private final String runName;
    private final Map<String, String> configMapContents;

    public PipelineConfigMapVolume(final List<PipelineInput> pipelineInputs, final KubernetesClient kubernetesClient,
            final String runName) {
        this.kubernetesClient = kubernetesClient;
        this.runName = runName;
        this.configMapContents = inputAsConfigMap(pipelineInputs, runName);
    }

    public Volume asKubernetes() {
        String name = runName + "-" + SAMPLES;
        kubernetesClient.configMaps()
                .inNamespace(KubernetesCluster.NAMESPACE)
                .withName(name)
                .createOrReplace(new ConfigMapBuilder().addToData(configMapContents)
                        .withNewMetadata()
                        .withName(name)
                        .withNamespace(KubernetesCluster.NAMESPACE)
                        .endMetadata()
                        .build());
        return new VolumeBuilder().withName(name).editOrNewConfigMap().withName(name).endConfigMap().build();
    }

    public Set<String> getConfigMapKeys() {
        return Collections.unmodifiableSet(configMapContents.keySet());
    }

    private static Map<String, String> inputAsConfigMap(final List<PipelineInput> pipelineInputs, final String runName) {
        return pipelineInputs.stream()
                .collect(Collectors.toMap(pipelineInput -> toLabel(pipelineInput, runName), PipelineConfigMapVolume::toJson));
    }

    private static String toLabel(final PipelineInput pipelineInput, final String runName) {
        return pipelineInput.tumor()
                .or(pipelineInput::reference)
                .map(sampleInput -> KubernetesUtil.toValidRFC1123Label(sampleInput.name(), runName))
                .orElseThrow(() -> new IllegalArgumentException("Need to specify either tumor or reference."));
    }

    private static String toJson(final PipelineInput pipelineInput) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        try {
            return objectMapper.writeValueAsString(pipelineInput);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
