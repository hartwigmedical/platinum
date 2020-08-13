package com.hartwig.platinum.kubernetes;

import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.batch.JobSpec;
import io.fabric8.kubernetes.api.model.batch.JobSpecBuilder;

public class PipelineJob {
    JobSpec create(final Container container, final Volume config, final Volume secretKey) {
        JobSpecBuilder jobSpecBuilder = new JobSpecBuilder();
        return jobSpecBuilder.withNewTemplate()
                .withNewSpec()
                .withContainers(container)
                .withRestartPolicy("Never")
                .withVolumes(List.of(config, secretKey))
                .endSpec()
                .endTemplate()
                .build();
    }
}
