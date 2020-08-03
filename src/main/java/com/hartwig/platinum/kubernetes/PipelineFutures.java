package com.hartwig.platinum.kubernetes;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.hartwig.platinum.jobs.PipelineResult;

public class PipelineFutures {

    public List<PipelineResult> get() {
        try {
            // wait for all the results, maybe print out something periodically?
            new CountDownLatch(1).wait();
            return Collections.emptyList();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
