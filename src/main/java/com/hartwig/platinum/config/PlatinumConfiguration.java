package com.hartwig.platinum.config;

import java.io.File;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(jdkOnly = true)
@JsonDeserialize(as = ImmutablePlatinumConfiguration.class)
public interface PlatinumConfiguration {
    Map<String, String> pipelineArguments();

    String runName();

    String dataDirectory();

    OutputConfiguration outputConfiguration();

    static PlatinumConfiguration from(final String runName, final String inputJson, final String dataDirectory, final String location) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        try {
            Map<String, String> arguments = objectMapper.readValue(new File(inputJson), new TypeReference<Map<String, String>>() {});
            ImmutableOutputConfiguration.Builder outputConfiguration = OutputConfiguration.builder().location(location);
            if (arguments.containsKey("cmek")) {
                outputConfiguration.cmek(arguments.get("cmek"));
            }
            return ImmutablePlatinumConfiguration.builder().runName(runName).dataDirectory(dataDirectory)
                    .outputConfiguration(outputConfiguration.build()).pipelineArguments(arguments).build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise configuration", e);
        }
    }
}

