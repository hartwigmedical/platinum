package com.hartwig.platinum;

import java.util.List;

import com.hartwig.platinum.jobs.Pipeline;
import com.hartwig.platinum.jobs.PipelineResult;

import org.immutables.value.Value;

@Value.Immutable
public interface PlatinumResult {

    int numSuccess();

    int numFailure();

    List<Pipeline> results();

    static PlatinumResult of(final List<PipelineResult> results) {
        return null;
    }
}
