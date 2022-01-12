package com.hartwig.platinum.p5sample;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableSample.class)
@Value.Style(jdkOnly = true)
public interface Sample {

    String name();

    Optional<String> bam();

    List<Lane> lanes();

    List<String> primaryTumorDoids();

    static ImmutableSample.Builder builder(final String name) {
        return ImmutableSample.builder().name(name);
    }
}