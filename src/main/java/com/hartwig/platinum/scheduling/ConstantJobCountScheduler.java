package com.hartwig.platinum.scheduling;

import java.util.ArrayList;
import java.util.List;

import com.hartwig.platinum.kubernetes.JobSubmitter;
import com.hartwig.platinum.kubernetes.KubernetesClientProxy;
import com.hartwig.platinum.kubernetes.pipeline.PipelineJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.client.KubernetesClientException;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

public class ConstantJobCountScheduler implements JobScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConstantJobCountScheduler.class);
    private static final int INITIAL_LOGGING_INTERVAL_MILLISECONDS = 60 * 1000;
    private final JobSubmitter jobSubmitter;
    private final int jobCount;
    private final List<PipelineJob> activeJobs;
    private final KubernetesClientProxy kubernetesClientProxy;
    private final Delay delayBetweenSubmissions;
    private final Delay pollingInterval;

    public ConstantJobCountScheduler(final JobSubmitter jobSubmitter, final KubernetesClientProxy kubernetesClientProxy, final int jobCount,
            final Delay delayBetweenSubmissions, final Delay pollingInterval) {
        this.jobSubmitter = jobSubmitter;
        this.kubernetesClientProxy = kubernetesClientProxy;
        this.jobCount = jobCount;
        this.delayBetweenSubmissions = delayBetweenSubmissions;
        this.pollingInterval = pollingInterval;

        activeJobs = new ArrayList<>(jobCount);
    }

    private static boolean jobIsNot(Integer status) {
        return status != null && status != 1;
    }

    @Override
    public void submit(final PipelineJob job) {
        while (activeJobs.size() >= jobCount) {
            List<PipelineJob> removedJobs = waitForCapacity();
            if (!removedJobs.isEmpty()) {
                activeJobs.removeAll(removedJobs);
            }
        }
        if (jobSubmitter.submit(job)) {
            activeJobs.add(job);
            delayBetweenSubmissions.threadSleep();
        }
    }

    private List<PipelineJob> waitForCapacity() {
        List<PipelineJob> removedJobs = new ArrayList<>();
        if (activeJobs.size() >= jobCount) {
            int millisecondsWaitedSoFar = 0;
            while (activeJobs.size() - removedJobs.size() >= jobCount) {
                for (PipelineJob activeJob : activeJobs) {
                    Failsafe.with(new RetryPolicy<>().handle(KubernetesClientException.class)
                            .withMaxRetries(2)
                            .onRetry(e -> kubernetesClientProxy.authorise())).run(() -> {
                        Job job = kubernetesClientProxy.jobs().withName(activeJob.getName()).get();
                        if (job == null) {
                            LOGGER.warn("Previously-created k8 job [{}] not found, will not schedule another in its place!",
                                    activeJob.getName());
                            removedJobs.add(activeJob);
                        } else {
                            if (jobIsNot(job.getStatus().getActive())) {
                                if (jobIsNot(job.getStatus().getFailed())) {
                                    removedJobs.add(activeJob);
                                } else {
                                    kubernetesClientProxy.jobs().delete(job);
                                    if (jobSubmitter.submit(activeJob)) {
                                        LOGGER.info("Resubmitted failed job [{}]", activeJob.getName());
                                    } else {
                                        removedJobs.add(activeJob);
                                    }
                                }
                            }
                        }
                    });
                }
                pollingInterval.threadSleep();
                millisecondsWaitedSoFar += pollingInterval.toMillis();
                if (millisecondsWaitedSoFar >= INITIAL_LOGGING_INTERVAL_MILLISECONDS) {
                    LOGGER.info("Continuing to wait for capacity");
                    millisecondsWaitedSoFar = 0;
                }
            }
        }
        return removedJobs;
    }
}
