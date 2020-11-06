package com.hartwig.platinum.kubernetes;

import static com.hartwig.platinum.kubernetes.KubernetesCluster.NAMESPACE;

import io.fabric8.kubernetes.api.model.batch.JobSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

public class JobScheduler {

    private final KubernetesClient kubernetesClient;

    public JobScheduler(final KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    void submit(final PipelineJob job) {
        JobSpec spec = job.asKubernetes();
        kubernetesClient.batch()
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
