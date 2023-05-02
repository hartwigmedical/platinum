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
import com.hartwig.platinum.kubernetes.PipelineConfigMapVolume.PipelineConfigMapVolumeBuilder;

import io.fabric8.kubernetes.api.model.Volume;

public class PipelineConfigMaps {

    private final PipelineConfigMapVolumeBuilder pipelineConfigMapVolumeBuilder;
    private final String runName;
    private final Map<String, String> configsKeyedBySample;

    public PipelineConfigMaps(final List<PipelineInput> pipelineInputs, final PipelineConfigMapVolumeBuilder pipelineConfigMapVolumeBuilder,
            final String runName) {
        this.pipelineConfigMapVolumeBuilder = pipelineConfigMapVolumeBuilder;
        this.runName = runName;
        this.configsKeyedBySample = inputAsMapKeyedBySample(pipelineInputs);
    }

    public Volume forSample(String sample) {
        return pipelineConfigMapVolumeBuilder.of(runName, sample, configsKeyedBySample.get(sample)).asKubernetes();
    }

    public Set<String> getConfigMapKeys() {
        return Collections.unmodifiableSet(configsKeyedBySample.keySet());
    }

    private static Map<String, String> inputAsMapKeyedBySample(final List<PipelineInput> pipelineInputs) {
        return pipelineInputs.stream()
                .collect(Collectors.toMap(PipelineConfigMaps::toLabel, PipelineConfigMaps::toJson));
    }

    private static String toLabel(final PipelineInput pipelineInput) {
        return pipelineInput.tumor()
                .or(pipelineInput::reference)
                .map(sampleInput -> KubernetesUtil.toValidRFC1123Label(sampleInput.name()))
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
