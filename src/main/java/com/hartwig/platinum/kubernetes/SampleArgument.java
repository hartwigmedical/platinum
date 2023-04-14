package com.hartwig.platinum.kubernetes;

import java.util.Map;

import com.hartwig.pdl.PipelineInput;

import org.immutables.value.Value;

@Value.Immutable
public interface SampleArgument {

    String id();

    Map<String, String> arguments();

    static SampleArgument sampleJson(final PipelineInput pipelineInput, final String runName) {
        String setName = pipelineInput.setName();
        return ImmutableSampleArgument.builder()
                .id(setName.toLowerCase())
                .putArguments("-sample_json", String.format("samples/%s-%s", setName.toLowerCase(), runName))
                .putArguments("-run_tag", runName)
                .build();
    }
}