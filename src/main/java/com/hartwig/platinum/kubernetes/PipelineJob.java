package com.hartwig.platinum.kubernetes;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodTemplateSpecFluent;
import io.fabric8.kubernetes.api.model.Toleration;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.batch.JobSpec;
import io.fabric8.kubernetes.api.model.batch.JobSpecBuilder;
import io.fabric8.kubernetes.api.model.batch.JobSpecFluent;

public class PipelineJob implements KubernetesComponent<JobSpec> {

    public Container getContainer() {
        return container;
    }

    private final Container container;
    private final List<Volume> volumes;
    private final String name;
    private final TargetNodePool nodePool;

    public PipelineJob(final String run, final String sample, final Container container, final List<Volume> volumes,
            final TargetNodePool nodePool) {
        this.container = container;
        this.volumes = volumes;
        this.nodePool = nodePool;
        this.name = sample + "-" + run;
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
        final PodTemplateSpecFluent.SpecNested<JobSpecFluent.TemplateNested<JobSpecBuilder>> dsl =
                jobSpecBuilder.withNewTemplate().withNewSpec().withContainers(container).withRestartPolicy("Never").withVolumes(volumes);
        if (!nodePool.isDefault()) {
            dsl.withNodeSelector(Map.of("pool", nodePool.name()))
                    .withTolerations(List.of(new Toleration("NoSchedule", "reserved-pool", "Equal", null, "true")));
        }
        JobSpec spec = dsl.endSpec().endTemplate().build();
        spec.setAdditionalProperty("backoffLimit", 1);
        return spec;
    }
}