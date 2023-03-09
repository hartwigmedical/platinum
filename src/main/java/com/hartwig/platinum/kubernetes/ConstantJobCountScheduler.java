package com.hartwig.platinum.kubernetes;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

public class ConstantJobCountScheduler implements JobScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConstantJobCountScheduler.class);
    private static final int SECONDS_BETWEEN_SUBMISSIONS = 1000;
    private static final int SECONDS_BETWEEN_POLLS = 10 * 1000;
    private static final int INITIAL_LOGGING_INTERVAL_SECONDS = 60 * 1000;

    private final JobSubmitter jobSubmitter;
    private final int jobCount;
    private final List<PipelineJob> activeJobs;
    private KubernetesClient kubernetesClient;

    public ConstantJobCountScheduler(final JobSubmitter jobSubmitter, final KubernetesClient kubernetesClient, final int jobCount) {
        this.jobSubmitter = jobSubmitter;
        this.kubernetesClient = kubernetesClient;
        this.jobCount = jobCount;

        activeJobs = new ArrayList<>(jobCount);
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
            delay(SECONDS_BETWEEN_SUBMISSIONS);
        }
    }

    private List<PipelineJob> waitForCapacity() {
        List<PipelineJob> removedJobs = new ArrayList<>();
        try {
            if (activeJobs.size() >= jobCount) {
                int millisecondsWaitedSoFar = 0;
                int nextLoggingInterval = INITIAL_LOGGING_INTERVAL_SECONDS;
                while (activeJobs.size() - removedJobs.size() >= jobCount) {
                    for (PipelineJob activeJob : activeJobs) {
                        Job job = kubernetesClient.batch().jobs().inNamespace(KubernetesCluster.NAMESPACE).withName(activeJob.getName()).get();
                        if (job == null) {
                            LOGGER.warn("Previously-created k8 job [{}] not found, will not schedule another in its place!", activeJob.getName());
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
                    delay(SECONDS_BETWEEN_POLLS);
                    millisecondsWaitedSoFar += SECONDS_BETWEEN_POLLS;
                    if (millisecondsWaitedSoFar >= nextLoggingInterval) {
                        nextLoggingInterval *= 1.25;
                        LOGGER.info("Continuing to wait for capacity");
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

    private void delay(int delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            LOGGER.error("Thread interrupted", e);
            Thread.interrupted();
        }
    }
}
