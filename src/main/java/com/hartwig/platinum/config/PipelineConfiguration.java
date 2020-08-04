package com.hartwig.platinum.config;

import java.util.Map;

import org.immutables.value.Value;

@Value.Immutable
public interface PipelineConfiguration {

    String sampleName();

    Map<String, String> arguments();

    static ImmutablePipelineConfiguration.Builder builder() {
        return ImmutablePipelineConfiguration.builder();
    }
}
