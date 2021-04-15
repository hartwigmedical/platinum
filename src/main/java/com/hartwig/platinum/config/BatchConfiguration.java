package com.hartwig.platinum.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableBatchConfiguration.class)
public interface BatchConfiguration {

    Integer size();

    Integer delay();
}
