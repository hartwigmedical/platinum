package com.hartwig.platinum.p5sample;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableLane.class)
public interface Lane {

    String laneNumber();

    String firstOfPairPath();

    String secondOfPairPath();

    static ImmutableLane.Builder builder() {
        return ImmutableLane.builder();
    }
}