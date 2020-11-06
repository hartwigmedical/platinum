package com.hartwig.platinum.kubernetes;

import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.batch.JobSpec;
import io.fabric8.kubernetes.api.model.batch.JobSpecBuilder;

public class PipelineJob implements KubernetesComponent<JobSpec> {

    public Container getContainer() {
        return container;
    }

    private final Container container;
    private final List<Volume> volumes;
    private final String name;

    public PipelineJob(final String name, final Container container, final List<Volume> volumes) {
        this.container = container;
        this.volumes = volumes;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Volume> getVolumes() {
        return volumes;
    }

    @Override
    public JobSpec asKubernetes() {
        JobSpecBuilder jobSpecBuilder = new JobSpecBuilder();
        return jobSpecBuilder.withNewTemplate()
                .withNewSpec()
                .withContainers(container)
                .withRestartPolicy("Never")
                .withVolumes(volumes)
                .endSpec()
                .endTemplate()
                .build();
    }
}
