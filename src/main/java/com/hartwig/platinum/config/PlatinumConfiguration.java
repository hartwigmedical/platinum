package com.hartwig.platinum.config;

import java.io.File;
import java.io.IOException;
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
    default GcpConfiguration gcp() {
        return GcpConfiguration.builder().build();
    }

    Optional<String> cmek();

    Map<String, String> argumentOverrides();

    Map<String, JsonNode> samples();

    default PlatinumConfiguration withGcp(final GcpConfiguration gcp) {
        return builder().from(this).gcp(gcp).build();
    }

    static ImmutablePlatinumConfiguration.Builder builder() {
        return ImmutablePlatinumConfiguration.builder();
    }

    static PlatinumConfiguration from(final String inputFile) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        try {
            return objectMapper.readValue(new File(inputFile), new TypeReference<>() {
            });
        } catch (IOException ioe) {
            throw new RuntimeException("Could not parse input", ioe);
        }
    }
}

