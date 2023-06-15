package com.hartwig.platinum.kubernetes.pipeline;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.hartwig.platinum.kubernetes.KubernetesComponent;
import com.hartwig.platinum.kubernetes.TargetNodePool;

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
    private final Duration ttl;

    public PipelineJob(final String name, final Container container, final List<Volume> volumes,
            final TargetNodePool nodePool, final Duration ttl) {
        this.container = container;
        this.volumes = volumes;
        this.nodePool = nodePool;
        this.ttl = ttl;
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
        final PodTemplateSpecFluent.SpecNested<JobSpecFluent.TemplateNested<JobSpecBuilder>> dsl =
                jobSpecBuilder.withNewTemplate().withNewSpec().withContainers(container).withRestartPolicy("Never").withVolumes(volumes);
        if (!nodePool.isDefault()) {
            dsl.withNodeSelector(Map.of("pool", nodePool.name()))
                    .withTolerations(List.of(new Toleration("NoSchedule", "reserved-pool", "Equal", null, "true")));
        }
        JobSpec spec = dsl.endSpec().endTemplate().build();
        spec.setBackoffLimit(1);
        if (!ttl.equals(Duration.ZERO)) {
            spec.setTtlSecondsAfterFinished(Math.toIntExact(ttl.getSeconds()));
        }
        return spec;
    }
}