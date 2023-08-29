package com.hartwig.platinum.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Optional;

@JsonDeserialize(as = ImmutableServiceAccountConfiguration.class)
@Value.Style(jdkOnly = true)
@Value.Immutable
public interface ServiceAccountConfiguration {

    Optional<String> gcpEmailAddress();

    Optional<String> kubernetesServiceAccount();

    static ImmutableServiceAccountConfiguration.Builder builder() {
        return ImmutableServiceAccountConfiguration.builder();
    }
}
