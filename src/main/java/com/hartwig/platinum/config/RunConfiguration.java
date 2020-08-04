package com.hartwig.platinum.config;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import org.immutables.value.Value.Immutable;

@Immutable
public interface RunConfiguration {

    String runName();

    OutputConfiguration outputConfiguration();

    List<PipelineConfiguration> pipelines();

    static RunConfiguration from(final String runName, final String jsonPath) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());

        try {
            InputJson json = objectMapper.readValue(new File(jsonPath), new TypeReference<InputJson>(){});
            List<PipelineConfiguration> pipelineConfigurations = new ArrayList<>();
            if (json.samples().isEmpty()) {
                throw new IllegalArgumentException("No samples in input!");
            }
            new HashSet<>(json.samples()).forEach(sample -> {
                pipelineConfigurations.add(PipelineConfiguration.builder().sampleName(sample).arguments(json.pipelineArguments()).build());
            });
            return ImmutableRunConfiguration.builder().runName(runName).pipelines(pipelineConfigurations).build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse configuration", e);
        }
    }
}