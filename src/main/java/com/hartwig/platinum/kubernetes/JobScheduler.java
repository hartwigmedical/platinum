package com.hartwig.platinum.kubernetes;

import static com.hartwig.platinum.kubernetes.KubernetesCluster.NAMESPACE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

public class JobScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobScheduler.class);
    private final KubernetesClient kubernetesClient;

    public JobScheduler(final KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    Job submit(final PipelineJob job) {
        JobSpec spec = job.asKubernetes();
        Boolean delete = kubernetesClient.batch().jobs().inNamespace(NAMESPACE).withName(job.getName()).delete();
        if (delete) {
            LOGGER.info("Delete existing job [{}]", job.getName());
        }
        return kubernetesClient.batch()
                .jobs()
                .createNew()
                .withNewMetadata()
                .withName(job.getName())
                .withNamespace(NAMESPACE)
                .endMetadata()
                .withSpec(spec)
                .done();
    }
}
