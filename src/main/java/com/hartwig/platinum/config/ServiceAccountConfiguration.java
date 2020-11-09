package com.hartwig.platinum.config;

import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.immutables.value.Value;

@JsonDeserialize(as = ImmutableServiceAccountConfiguration.class)
@Value.Immutable
public interface ServiceAccountConfiguration {

    String name();

    Optional<String> existingSecret();
}
