package com.hartwig.platinum.kubernetes;

import static java.lang.String.format;

import java.util.Map;

import com.hartwig.platinum.p5sample.TumorNormalPair;

import org.immutables.value.Value;

@Value.Immutable
public interface SampleArgument {

    String id();

    Map<String, String> arguments();

    static SampleArgument sampleJson(final TumorNormalPair pair, final String runName) {
        return ImmutableSampleArgument.builder()
                .id(pair.name().toLowerCase())
                .putArguments("-sample_json", format("samples/%s-%s", pair.name().toLowerCase(), runName))
                .putArguments("-set_id", pair.name())
                .putArguments("-run_id", runName + pair.tumorIndex().map(ti -> "-" + ti).orElse(""))
                .build();
    }

    static SampleArgument biopsy(final String biopsy) {
        return ImmutableSampleArgument.builder().id(biopsy.toLowerCase()).putArguments("-biopsy", biopsy).build();
    }
}
