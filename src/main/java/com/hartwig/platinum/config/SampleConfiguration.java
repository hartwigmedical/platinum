package com.hartwig.platinum.config;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableSampleConfiguration.class)
public interface SampleConfiguration {

    String name();

    List<String> primaryTumorDoids();

    List<RawDataConfiguration> tumors();

    Optional<RawDataConfiguration> normal();

    static ImmutableSampleConfiguration.Builder builder() {
        return ImmutableSampleConfiguration.builder();
    }
}
