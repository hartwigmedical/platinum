package com.hartwig.platinum.kubernetes.scheduling;

import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.kubernetes.JobSubmitter;
import com.hartwig.platinum.kubernetes.PipelineJob;

public interface JobScheduler {
    void submit(final PipelineJob job);

    static JobScheduler fromConfiguration(PlatinumConfiguration configuration, JobSubmitter jobSubmitter) {
        return configuration.batch().map(c -> c.delay() == null ?
                        new ConstantJobCountScheduler(jobSubmitter, c.size(), Delay.forSeconds(1), Delay.forSeconds(10)) :
                        new TimedBatchScheduler(jobSubmitter, Delay.forMinutes(c.delay()), c.size()))
                .orElse(new ConstantJobCountScheduler(jobSubmitter, 1, Delay.forSeconds(1), Delay.forSeconds(10)));
    }
}
