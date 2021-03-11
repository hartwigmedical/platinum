package com.hartwig.platinum.p5sample;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.immutables.value.Value;

@JsonSerialize(as = ImmutableTumorNormalPair.class)
@Value.Immutable
public interface TumorNormalPair {

    @JsonIgnore
    String name();

    @JsonIgnore
    Optional<String> tumorIndex();

    Sample reference();

    Sample tumor();
}
