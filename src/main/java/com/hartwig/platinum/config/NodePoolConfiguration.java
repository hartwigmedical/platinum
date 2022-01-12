package com.hartwig.platinum.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableNodePoolConfiguration.class)
@Value.Style(jdkOnly = true)
public interface NodePoolConfiguration {

    String name();
}