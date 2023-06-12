package com.hartwig.platinum.kubernetes;

import static java.util.List.of;

import static com.hartwig.platinum.kubernetes.KubernetesCluster.NAMESPACE;

import com.hartwig.platinum.config.GcpConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.JobSpec;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

public class JobSubmitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobSubmitter.class);
    private KubernetesClient kubernetesClient;
    private final String clusterName;
    private final GcpConfiguration gcpConfiguration;
    private final boolean retryFailed;

    public JobSubmitter(final String clusterName, final GcpConfiguration gcpConfiguration, boolean retryFailed) {
        this.clusterName = clusterName;
        this.gcpConfiguration = gcpConfiguration;
        this.retryFailed = retryFailed;
        this.kubernetesClient = this.refreshClient();
    }

    public boolean submit(final PipelineJob job) {
        try {
            JobSpec spec = job.asKubernetes();
            Job existing = kubernetesClient.batch().jobs().inNamespace(NAMESPACE).withName(job.getName()).get();
            if (existing == null) {
                return submit(job, spec);
            } else if (existing.getStatus().getFailed() == null || existing.getStatus().getFailed() == 0) {
                LOGGER.info("Job [{}] existed and completed successfully, skipping", job.getName());
                return false;
            } else {
                if (retryFailed) {
                    LOGGER.info("Job [{}] existed but failed, restarting", job.getName());
                    kubernetesClient.batch().jobs().delete(existing);
                    return submit(job, spec);
                } else {
                    LOGGER.info("Job [{}] existed but failed, skipping", job.getName());
                    return false;
                }
            }
        } catch (KubernetesClientException e) {
            kubernetesClient = refreshClient();
            LOGGER.info("Refreshed client, resubmitting job [{}]", job.getName());
            return submit(job);
        }
    }

    private boolean submit(final PipelineJob job, final JobSpec spec) {
        try {
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
        } catch (KubernetesClientException e) {
            kubernetesClient = refreshClient();
            LOGGER.info("Refreshed client, resubmitting job [{}]", job.getName());
            return submit(job, spec);
        }
    }

    public KubernetesClient refreshClient() {
        ProcessRunner processRunner = new ProcessRunner();
        if (!processRunner.execute(of("gcloud",
                "container",
                "clusters",
                "get-credentials",
                clusterName,
                "--region",
                gcpConfiguration.regionOrThrow(),
                "--project",
                gcpConfiguration.projectOrThrow()))) {
            throw new RuntimeException("Failed to get credentials for cluster");
        }
        if (!processRunner.execute(of("kubectl", "get", "secrets"))) {
            throw new RuntimeException("Failed to run kubectl command against cluster");
        }
        return new DefaultKubernetesClient();
    }
}
