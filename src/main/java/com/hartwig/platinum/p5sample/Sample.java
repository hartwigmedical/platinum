package com.hartwig.platinum.p5sample;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableSample.class)
@Value.Style(jdkOnly = true)
public interface Sample {

    String name();

    List<Lane> lanes();

    static ImmutableSample.Builder builder(final String name) {
        return ImmutableSample.builder().name(name);
    }
}