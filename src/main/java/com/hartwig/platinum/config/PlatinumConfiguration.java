package com.hartwig.platinum.config;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(jdkOnly = true)
@JsonDeserialize(as = ImmutablePlatinumConfiguration.class)
public interface PlatinumConfiguration {

    @Value.Default
    default String image() {
        return "eu.gcr.io/hmf-images/pipeline5:platinum";
    }

    Optional<String> cmek();

    Optional<String> serviceAccount();

    Optional<String> apiUrl();

    Map<String, String> argumentOverrides();

    Map<String, JsonNode> samples();

    List<String> biopsies();

    static ImmutablePlatinumConfiguration.Builder builder() {
        return ImmutablePlatinumConfiguration.builder();
    }

    static PlatinumConfiguration from(File inputFile) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        try {
            PlatinumConfiguration platinumConfiguration = objectMapper.readValue(inputFile, new TypeReference<>() {
            });
            if (!platinumConfiguration.samples().isEmpty() && !platinumConfiguration.biopsies().isEmpty()) {
                throw new IllegalArgumentException("Cannot specify both a list of sample jsons and a list of biopsies. "
                        + "Split this configuration into two platinum runs.");
            }
            return platinumConfiguration;
        } catch (IOException ioe) {
            throw new RuntimeException("Could not parse input", ioe);
        }

    }
}

