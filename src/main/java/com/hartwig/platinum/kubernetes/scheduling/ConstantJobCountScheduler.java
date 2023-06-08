package com.hartwig.platinum.kubernetes.scheduling;

import java.util.ArrayList;
import java.util.List;

import com.hartwig.platinum.kubernetes.JobSubmitter;
import com.hartwig.platinum.kubernetes.KubernetesCluster;
import com.hartwig.platinum.kubernetes.PipelineJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

public class ConstantJobCountScheduler implements JobScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConstantJobCountScheduler.class);
    private static final int INITIAL_LOGGING_INTERVAL_MILLISECONDS = 60 * 1000;

    private final JobSubmitter jobSubmitter;
    private final int jobCount;
    private final List<PipelineJob> activeJobs;
    private KubernetesClient kubernetesClient;
    private final Delay delayBetweenSubmissions;
    private final Delay pollingInterval;

    public ConstantJobCountScheduler(final JobSubmitter jobSubmitter, final int jobCount, final Delay delayBetweenSubmissions,
            final Delay pollingInterval) {
        this.jobSubmitter = jobSubmitter;
        this.jobCount = jobCount;
        this.delayBetweenSubmissions = delayBetweenSubmissions;
        this.pollingInterval = pollingInterval;

        activeJobs = new ArrayList<>(jobCount);
        kubernetesClient = jobSubmitter.refreshClient();
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
        try {
            if (activeJobs.size() >= jobCount) {
                int millisecondsWaitedSoFar = 0;
                while (activeJobs.size() - removedJobs.size() >= jobCount) {
                    for (PipelineJob activeJob : activeJobs) {
                        Job job = kubernetesClient.batch()
                                .jobs()
                                .inNamespace(KubernetesCluster.NAMESPACE)
                                .withName(activeJob.getName())
                                .get();
                        if (job == null) {
                            LOGGER.warn("Previously-created k8 job [{}] not found, will not schedule another in its place!",
                                    activeJob.getName());
                        } else {
                            if (!jobIs(job.getStatus().getActive())) {
                                if (!jobIs(job.getStatus().getFailed())) {
                                    removedJobs.add(activeJob);
                                } else {
                                    kubernetesClient.batch().jobs().inNamespace(KubernetesCluster.NAMESPACE).delete(job);
                                    if (jobSubmitter.submit(activeJob)) {
                                        LOGGER.info("Resubmitted failed job [{}]", activeJob.getName());
                                    } else {
                                        removedJobs.add(activeJob);
                                    }
                                }
                            }
                        }
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
        } catch (KubernetesClientException e) {
            kubernetesClient = jobSubmitter.refreshClient();
            return removedJobs;
        }
    }

    private boolean jobIs(Integer status) {
        return status != null && status == 1;
    }
}
