package com.hartwig.platinum.config;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableSampleConfiguration.class)
public interface SampleConfiguration {

    String name();

    List<String> primaryTumorDoids();

    List<RawDataConfiguration> tumors();

    RawDataConfiguration normal();

    static ImmutableSampleConfiguration.Builder builder() {
        return ImmutableSampleConfiguration.builder();
    }
}
