package com.hartwig.platinum.kubernetes;

import static com.hartwig.platinum.kubernetes.KubernetesCluster.NAMESPACE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.JobSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

public class JobScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobScheduler.class);
    private final KubernetesClient kubernetesClient;

    public JobScheduler(final KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    boolean submit(final PipelineJob job) {
        JobSpec spec = job.asKubernetes();
        Job existing = kubernetesClient.batch().jobs().inNamespace(NAMESPACE).withName(job.getName()).get();
        if (existing == null) {
            return submit(job, spec);
        } else if (existing.getStatus().getFailed() == null || existing.getStatus().getFailed() == 0) {
            LOGGER.info("Job [{}] alread existed, skipping", job.getName());
            return false;
        } else {
            LOGGER.info("Job [{}] existed but failed. Restarting", job.getName());
            kubernetesClient.batch().jobs().delete(existing);
            return submit(job, spec);
        }
    }

    private boolean submit(final PipelineJob job, final JobSpec spec) {
        kubernetesClient.batch()
                .jobs()
                .create(new JobBuilder().withNewMetadata()
                        .withName(job.getName())
                        .withNamespace(NAMESPACE)
                        .endMetadata()
                        .withSpec(spec)
                        .build());
        LOGGER.info("Submitted [{}]", job.getName());
        return true;
    }
}
