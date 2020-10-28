package com.hartwig.platinum.kubernetes;

import org.immutables.value.Value;

@Value.Immutable
public interface SampleArgument {

    String argument();

    String value();

    static SampleArgument sampleJson(final String sample){
        //  .put("-sample_json", format("%s/%s", samplesPath, sample))
        return null;
    }

    static SampleArgument biopsy(final String biopsy){
        //  .put("-sample_json", format("%s/%s", samplesPath, sample))
        return null;
    }
}
