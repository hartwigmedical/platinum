package com.hartwig.platinum.config;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.immutables.value.Value.Style;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Immutable
@Style(jdkOnly = true)
@JsonDeserialize(as = ImmutableInputJson.class)
public interface InputJson {
    @Parameter
    List<String> samples();

    @Parameter
    Map<String, String> pipelineArguments();
}

