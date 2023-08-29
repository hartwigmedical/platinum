package com.hartwig.platinum.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.immutables.value.Value;

@JsonDeserialize(as = ImmutableServiceAccountConfiguration.class)
@Value.Style(jdkOnly = true)
@Value.Immutable
public interface ServiceAccountConfiguration {

    String gcpEmailAddress();

    String kubernetesServiceAccount();

    static ImmutableServiceAccountConfiguration.Builder builder() {
        return ImmutableServiceAccountConfiguration.builder();
    }
}
