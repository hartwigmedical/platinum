package com.hartwig.platinum.kubernetes;

import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.batch.JobSpec;
import io.fabric8.kubernetes.api.model.batch.JobSpecBuilder;

public class PipelineJob implements KubernetesComponent<JobSpec> {

    private final Container container;
    private final Volume config;
    private final Volume secret;
    private final Volume jks;

    public PipelineJob(final Container container, final Volume config, final Volume secret, final Volume jks) {
        this.container = container;
        this.config = config;
        this.secret = secret;
        this.jks = jks;
    }

    @Override
    public JobSpec asKubernetes() {
        JobSpecBuilder jobSpecBuilder = new JobSpecBuilder();
        return jobSpecBuilder.withNewTemplate()
                .withNewSpec()
                .withContainers(container)
                .withRestartPolicy("Never")
                .withVolumes(List.of(config, secret, jks))
                .endSpec()
                .endTemplate()
                .build();
    }
}
