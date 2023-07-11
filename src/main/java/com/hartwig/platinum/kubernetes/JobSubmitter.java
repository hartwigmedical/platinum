package com.hartwig.platinum.kubernetes;

import com.hartwig.platinum.kubernetes.pipeline.PipelineJob;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.JobSpec;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hartwig.platinum.kubernetes.KubernetesCluster.NAMESPACE;

public class JobSubmitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobSubmitter.class);
    private static final String JOB_COMPLETE_STATUS = "Complete";
    private static final String JOB_FAILED_STATUS = "Failed";

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
        } else if (jobIs(existing, JOB_COMPLETE_STATUS)) {
            LOGGER.info("Job [{}] existed and completed successfully, skipping", job.getName());
            return false;
        } else if (jobIs(existing, JOB_FAILED_STATUS)) {
            if (retryFailed) {
                LOGGER.info("Job [{}] existed but failed, restarting", job.getName());
                kubernetesClientProxy.jobs().delete(existing);
                return submit(job, spec);
            } else {
                LOGGER.info("Job [{}] existed but failed, skipping", job.getName());
                return false;
            }
        } else {
            LOGGER.info("Job [{}] is already running", job.getName());
            return true;
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
            kubernetesClientProxy.authorise();
            return submit(job, spec);
        }
    }

    public static boolean jobIs(Job job, String stateName) {
        return job.getStatus().getConditions().stream().anyMatch(c -> stateName.equalsIgnoreCase(c.getType()) && "True".equals(c.getStatus()));
    }
}
