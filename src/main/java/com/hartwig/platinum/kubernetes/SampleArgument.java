package com.hartwig.platinum.kubernetes;

import java.util.Map;

import org.immutables.value.Value;

@Value.Immutable
public interface SampleArgument {

    String id();

    Map<String, String> arguments();

    static SampleArgument sampleJson(final String sample) {
        return ImmutableSampleArgument.builder()
                .id(sample.toLowerCase())
                .putArguments("-sample_json", sample)
                .putArguments("-set_id", sample)
                .build();
    }

    static SampleArgument biopsy(final String biopsy) {
        return ImmutableSampleArgument.builder().id(biopsy.toLowerCase()).putArguments("-biopsy", biopsy).build();
    }
}
