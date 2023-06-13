package com.hartwig.platinum.scheduling;

import com.hartwig.platinum.kubernetes.JobSubmitter;
import com.hartwig.platinum.kubernetes.pipeline.PipelineJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimedBatchScheduler implements JobScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimedBatchScheduler.class);
    private final JobSubmitter jobSubmitter;
    private final Delay delay;
    private final int batchSize;
    private int numSubmitted;

    public TimedBatchScheduler(final JobSubmitter jobSubmitter, final Delay delay, final int batchSize) {
        this.jobSubmitter = jobSubmitter;
        this.delay = delay;
        this.batchSize = batchSize;
    }

    @Override
    public void submit(final PipelineJob job) {
        if (jobSubmitter.submit(job)) {
            numSubmitted++;
            if (numSubmitted % batchSize == 0) {
                LOGGER.info("Batch [{}] scheduled, waiting [{}]",
                        (numSubmitted / batchSize),
                        delay.toString());
                delay.threadSleep();
            }
        }
    }
}
