package com.hartwig.platinum.kubernetes.pipeline;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.hartwig.platinum.kubernetes.KubernetesComponent;
import com.hartwig.platinum.kubernetes.KubernetesUtil;
import com.hartwig.platinum.kubernetes.TargetNodePool;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodTemplateSpecFluent;
import io.fabric8.kubernetes.api.model.Toleration;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpecBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpecFluent;

public class PipelineJob implements KubernetesComponent<JobSpec> {

    private final Container container;
    private final List<Volume> volumes;
    private final String name;
    private final String serviceAccountName;
    private final TargetNodePool nodePool;
    private final Duration ttl;

    public PipelineJob(final String name, final Container container, final List<Volume> volumes, final String serviceAccountName,
            final TargetNodePool nodePool, final Duration ttl) {
        this.container = container;
        this.volumes = volumes;
        this.serviceAccountName = serviceAccountName;
        this.nodePool = nodePool;
        this.ttl = ttl;
        this.name = KubernetesUtil.toValidRFC1123Label(name);
    }

    public Container getContainer() {
        return container;
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
        final PodTemplateSpecFluent<JobSpecFluent<JobSpecBuilder>.TemplateNested<JobSpecBuilder>>.SpecNested<JobSpecFluent<JobSpecBuilder>.TemplateNested<JobSpecBuilder>>
                dsl = jobSpecBuilder.withNewTemplate()
                .withNewSpec()
                .withContainers(container)
                .withRestartPolicy("Never")
                .withVolumes(volumes)
                .withServiceAccountName(serviceAccountName)
                .withNodeSelector(Map.of("iam.gke.io/gke-metadata-server-enabled", "true"));
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