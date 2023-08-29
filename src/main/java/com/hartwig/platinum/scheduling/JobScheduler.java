package com.hartwig.platinum.scheduling;

import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.kubernetes.JobSubmitter;
import com.hartwig.platinum.kubernetes.KubernetesClientProxy;
import com.hartwig.platinum.kubernetes.pipeline.PipelineJob;

public interface JobScheduler {
    Delay DELAY_BETWEEN_SUBMISSIONS = Delay.forSeconds(1);
    Delay POLLING_INTERVAL = Delay.forSeconds(10);

    static JobScheduler fromConfiguration(PlatinumConfiguration configuration, JobSubmitter jobSubmitter,
            KubernetesClientProxy kubernetesClientProxy) {
        return configuration.batch()
                .map(c -> c.delay().isPresent()
                        ? new TimedBatchScheduler(jobSubmitter, Delay.forMinutes(c.delay().get()), c.size())
                        : new ConstantJobCountScheduler(jobSubmitter,
                                kubernetesClientProxy,
                                c.size(),
                                JobScheduler.DELAY_BETWEEN_SUBMISSIONS,
                                JobScheduler.POLLING_INTERVAL))
                .orElse(new ConstantJobCountScheduler(jobSubmitter,
                        kubernetesClientProxy,
                        Integer.MAX_VALUE,
                        JobScheduler.DELAY_BETWEEN_SUBMISSIONS,
                        JobScheduler.POLLING_INTERVAL));
    }

    void submit(final PipelineJob job);
}
