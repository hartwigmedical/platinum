package com.hartwig.platinum.kubernetes;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.hartwig.platinum.config.PipelineConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.batch.JobSpecBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class KubernetesCluster {
    private final String runName;
    private final String name;
    private final String namespace;
//    private final String jobName;
    private String outputBucket;

    private KubernetesCluster(final String runName, final String namespace, final String outputBucket) {
        this.runName = runName.toLowerCase();
        this.namespace = namespace;
        this.name = format("platinum-%s-cluster", this.runName);
        this.outputBucket = outputBucket;
    }

    public void submit(final PlatinumConfiguration configuration) {
        List<PipelineConfiguration> pipelines = new ArrayList<>();
        for (String sample: configuration.samples().keySet()) {
            pipelines.add(PipelineConfiguration.builder()
                    .sampleName(sample)
                    .putAllArguments(configuration.pipelineArguments())
                    .putArguments("-set_id", sample)
                    .putArguments("-sample_json", format("/samples/%s", sample))
                    .putArguments("-patient_report_bucket", outputBucket)
                    .putArguments("-private_key_path", "/secrets/compute-key")
                    .build());
        }

        KubernetesClient client = new DefaultKubernetesClient();

        for (PipelineConfiguration pipeline: pipelines) {
            String containerName = format("%s-%s", namespace, pipeline.sampleName()).toLowerCase();
            Container container = new Container();
            container.setImage("hartwigmedicalfoundation/pipeline5:5.13.1677");
            container.setName(containerName);
            List<String> arguments = new ArrayList<>();
            arguments.add("/pipeline5.sh");
            for (Entry<String, String> entry : pipeline.arguments().entrySet()) {
                arguments.add(entry.getKey());
                arguments.add(entry.getValue());
            }
            container.setCommand(arguments);
            container.setVolumeMounts(List.of(new VolumeMountBuilder().withMountPath("/samples").withName("samples").build(),
                    new VolumeMountBuilder().withMountPath("/secrets").withName("compute-key").build()));

            String jobName = format("platinum-%s-%s", this.runName, pipeline.sampleName()).toLowerCase();
            JobSpecBuilder jobSpecBuilder = new JobSpecBuilder();
            jobSpecBuilder.withNewTemplate().withNewSpec()
                    .withContainers(container)
                    .withRestartPolicy("Never")
                    .withVolumes(List.of(
                            new VolumeBuilder().withName("samples").editOrNewConfigMap().withName("samples").endConfigMap().build(),
                            new VolumeBuilder().withName("compute-key").editOrNewSecret().withSecretName("compute-key").endSecret().build()))
                    .endSpec()
                    .endTemplate();
            client.batch().jobs().inNamespace(namespace).createNew().withNewMetadata().withName(jobName).endMetadata().withSpec(jobSpecBuilder.build()).done();
        }
    }

    public static KubernetesCluster findOrCreate(final String runName, final String namespace, final String outputBucket) {
        try {
            return new KubernetesCluster(runName, namespace, outputBucket);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create cluster", e);
        }
    }
}
