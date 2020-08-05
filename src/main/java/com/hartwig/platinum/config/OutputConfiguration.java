package com.hartwig.platinum.config;

import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.immutables.value.Value;
import org.immutables.value.Value.Style;

@Value.Immutable
@Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableOutputConfiguration.class)
public interface OutputConfiguration {

    String location();

    Optional<String> cmek();

    static ImmutableOutputConfiguration.Builder builder() {
        return ImmutableOutputConfiguration.builder();
    }
}
