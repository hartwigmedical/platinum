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

public class JobScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobScheduler.class);
    private KubernetesClient kubernetesClient;

    public JobScheduler(final KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    boolean submit(final PipelineJob job) {
        JobSpec spec = job.asKubernetes();
        Job existing = kubernetesClient.batch().jobs().inNamespace(NAMESPACE).withName(job.getName()).get();
        if (existing == null) {
            return submit(job, spec);
        } else if (existing.getStatus().getFailed() == null || existing.getStatus().getFailed() == 0) {
            LOGGER.info("Job [{}] existed and completed successfully, skipping", job.getName());
            return false;
        } else {
            LOGGER.info("Job [{}] existed but failed, restarting", job.getName());
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

    public void refresh(final String clusterName, final GcpConfiguration gcpConfiguration) {
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
        if (!processRunner.execute(of("kubectl", "get", "configmaps"))) {
            throw new RuntimeException("Failed to run kubectl command against cluster");
        }
        kubernetesClient = new DefaultKubernetesClient();
    }
}
