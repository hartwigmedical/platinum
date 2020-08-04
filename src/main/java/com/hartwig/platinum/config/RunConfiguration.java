package com.hartwig.platinum.config;

import java.util.List;

public interface RunConfiguration {

    String runName();

    OutputConfiguration outputConfiguration();

    List<PipelineConfiguration> pipelines();

    static RunConfiguration from(final String runName, final String gcsPath) {
        return null;
    }
}