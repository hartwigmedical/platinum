package com.hartwig.platinum.config;

import java.util.Optional;

import org.immutables.value.Value;

@Value.Immutable
public interface OutputConfiguration {

    String location();

    Optional<String> cmek();

    static ImmutableOutputConfiguration.Builder builder() {
        return ImmutableOutputConfiguration.builder();
    }
}
