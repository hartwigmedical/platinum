package com.hartwig.platinum.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableFastqConfiguration.class)
public interface FastqConfiguration {

    String read1();

    String read2();
}