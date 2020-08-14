package com.hartwig.platinum.config;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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

    OutputConfiguration outputConfiguration();

    Map<String, JsonNode> samples();

    static PlatinumConfiguration from(File inputFile) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        try {
            return objectMapper.readValue(inputFile, new TypeReference<>() {});
        } catch (IOException ioe) {
            throw new RuntimeException("Could not parse input", ioe);
        }
    }
}

