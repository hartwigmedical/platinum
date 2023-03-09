package com.hartwig.platinum.kubernetes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimedBatchScheduler implements JobScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimedBatchScheduler.class);
    private final JobSubmitter jobSubmitter;
    private final Delay delay;
    private final int batchSize;
    private final int delayMinutes;
    private int numSubmitted;

    public TimedBatchScheduler(final JobSubmitter jobSubmitter, final Delay delay, final int batchSize, final int delayMinutes) {
        this.jobSubmitter = jobSubmitter;
        this.delay = delay;
        this.batchSize = batchSize;
        this.delayMinutes = delayMinutes;
    }

    @Override
    public void submit(final PipelineJob job) {
        if (jobSubmitter.submit(job)) {
            numSubmitted++;
            if (numSubmitted % batchSize == 0) {
                LOGGER.info("Batch [{}] scheduled, waiting [{}] minutes",
                        (numSubmitted / batchSize),
                        delayMinutes);
                delay.forMinutes(delayMinutes);
            }
        }
    }
}
