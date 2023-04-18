package com.hartwig.platinum.kubernetes;

import java.util.Map;

import com.hartwig.pdl.PipelineInput;
import com.hartwig.pdl.SampleInput;

import org.immutables.value.Value;

@Value.Immutable
public interface SampleArgument {

    String id();

    Map<String, String> arguments();

    static SampleArgument sampleJson(final PipelineInput pipelineInput, final String runName) {
        String sampleName = pipelineInput.tumor()
                .or(pipelineInput::reference)
                .map(sampleInput -> KubernetesUtil.toValidRFC1123Label(sampleInput.name(), runName))
                .orElseThrow(() -> new IllegalArgumentException("Need to specify either tumor or reference."));
        return ImmutableSampleArgument.builder()
                .id(sampleName)
                .putArguments("-sample_json", "samples/" + sampleName)
                .putArguments("-run_tag", runName)
                .build();
    }
}