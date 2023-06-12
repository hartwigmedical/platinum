package com.hartwig.platinum.kubernetes;

import static com.hartwig.platinum.kubernetes.KubernetesCluster.NAMESPACE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.JobSpec;
import io.fabric8.kubernetes.client.KubernetesClientException;

public class JobSubmitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobSubmitter.class);
    private final KubernetesClientProxy kubernetesClientProxy;
    private final boolean retryFailed;

    public JobSubmitter(final KubernetesClientProxy kubernetesClientProxy, boolean retryFailed) {
        this.kubernetesClientProxy = kubernetesClientProxy;
        this.retryFailed = retryFailed;
    }

    public boolean submit(final PipelineJob job) {
        JobSpec spec = job.asKubernetes();
        Job existing = kubernetesClientProxy.jobs().withName(job.getName()).get();
        if (existing == null) {
            return submit(job, spec);
        } else if (existing.getStatus().getFailed() == null || existing.getStatus().getFailed() == 0) {
            LOGGER.info("Job [{}] existed and completed successfully, skipping", job.getName());
            return false;
        } else {
            if (retryFailed) {
                LOGGER.info("Job [{}] existed but failed, restarting", job.getName());
                kubernetesClientProxy.jobs().delete(existing);
                return submit(job, spec);
            } else {
                LOGGER.info("Job [{}] existed but failed, skipping", job.getName());
                return false;
            }
        }
    }

    private boolean submit(final PipelineJob job, final JobSpec spec) {
        try {
            kubernetesClientProxy.jobs()
                    .create(new JobBuilder().withNewMetadata()
                            .withName(job.getName())
                            .withNamespace(NAMESPACE)
                            .endMetadata()
                            .withSpec(spec)
                            .build());
            LOGGER.info("Submitted [{}]", job.getName());
            return true;
        } catch (KubernetesClientException e) {
            kubernetesClientProxy.reAuthorise();
            return submit(job, spec);
        }
    }
}
