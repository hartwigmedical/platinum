package com.hartwig.platinum.config;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableRawDataConfiguration.class)
public interface RawDataConfiguration {

    String name();

    List<FastqConfiguration> fastq();
}