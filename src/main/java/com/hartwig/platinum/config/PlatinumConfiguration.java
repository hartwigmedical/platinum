package com.hartwig.platinum.config;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(jdkOnly = true)
@JsonDeserialize(as = ImmutablePlatinumConfiguration.class)
public interface PlatinumConfiguration {
    Map<String, String> pipelineArguments();

    Optional<String> runName();

    OutputConfiguration outputConfiguration();

    Map<String, Map<String, ?>> samples();

    static ImmutablePlatinumConfiguration.Builder builder() {
        return ImmutablePlatinumConfiguration.builder();
    }
}

