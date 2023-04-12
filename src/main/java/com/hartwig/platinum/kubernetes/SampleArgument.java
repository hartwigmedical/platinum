package com.hartwig.platinum.kubernetes;

import static java.lang.String.format;

import java.util.Map;

import com.hartwig.pdl.PipelineInput;

import org.immutables.value.Value;

@Value.Immutable
public interface SampleArgument {

    String id();

    Map<String, String> arguments();

    static SampleArgument sampleJson(final PipelineInput pipelineInput, final String runName) {
        return ImmutableSampleArgument.builder()
                .id(pipelineInput.setName().orElseThrow().toLowerCase())
                .putArguments("-sample_json", format("%s/%s-%s", PipelineContainer.SAMPLES_PATH, pipelineInput.setName().orElseThrow().toLowerCase(), runName))
                .putArguments("-set_id", pipelineInput.setName().orElseThrow())
                .build();
    }

    static SampleArgument biopsy(final String biopsy, final String runName) {
        return ImmutableSampleArgument.builder()
                .id(biopsy.toLowerCase())
                .putArguments("-biopsy", biopsy)
                .putArguments("-run_id", runName)
                .build();
    }

    static SampleArgument runId(final String biopsy, final Long runId) {
        return ImmutableSampleArgument.builder()
                .id(biopsy.toLowerCase())
                .putArguments("-sbp_run_id", String.valueOf(runId))
                .putArguments("-biopsy", biopsy)
                .build();
    }
}