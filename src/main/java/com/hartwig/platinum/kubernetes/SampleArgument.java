package com.hartwig.platinum.kubernetes;

import java.util.Map;

import org.immutables.value.Value;

@Value.Immutable
public interface SampleArgument {

    String id();

    Map<String, String> arguments();

    static SampleArgument sampleJson(final String sampleName, final String runName) {
        return ImmutableSampleArgument.builder()
                .id(sampleName)
                .putArguments("-sample_json", "samples/" + sampleName)
                .putArguments("-run_tag", runName)
                .build();
    }
}