package com.hartwig.platinum.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableHmfConfiguration.class)
public interface HmfConfiguration {

    boolean isRerun();

    String apiUrl();
}
