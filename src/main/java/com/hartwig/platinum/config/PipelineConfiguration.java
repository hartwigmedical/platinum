package com.hartwig.platinum.config;

import com.hartwig.platinum.jobs.Pipeline;

import org.immutables.value.Value;

@Value.Immutable
public interface PipelineConfiguration {

    String sampleName();

    String json();
}
