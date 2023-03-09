package com.hartwig.platinum.kubernetes;

public interface JobScheduler {
    void submit(final PipelineJob job);
}
