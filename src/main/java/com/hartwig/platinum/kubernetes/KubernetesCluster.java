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
import io.fabric8.kubernetes.client.KubernetesClient;

public class KubernetesCluster {
    private final String runName;
    private final String configMapName;
    private final String jsonKeySecret;
    private final String outputBucket;
    private final KubernetesClient kubernetesClient;

    private final static String SAMPLES_PATH = "/samples";
    private final static String SECRETS_PATH = "/secrets";

    private KubernetesCluster(final String runName, final String configMapName, final String jsonKeySecret, final String outputBucket,
            final KubernetesClient kubernetesClient) {
        this.runName = runName.toLowerCase();
        this.configMapName = configMapName;
        this.jsonKeySecret = jsonKeySecret;
        this.outputBucket = outputBucket;
        this.kubernetesClient = kubernetesClient;
    }

    public void submit(final PlatinumConfiguration configuration) {
        List<PipelineConfiguration> pipelines = new ArrayList<>();
        for (String sample: configuration.samples().keySet()) {
            pipelines.add(PipelineConfiguration.builder()
                    .sampleName(sample)
                    .putAllArguments(configuration.pipelineArguments())
                    .putArguments("-set_id", sample)
                    .putArguments("-sample_json", format("%s/%s", SAMPLES_PATH, sample))
                    .putArguments("-patient_report_bucket", outputBucket)
                    .putArguments("-private_key_path", format("%s/%s", SECRETS_PATH, jsonKeySecret))
                    .build());
        }

        for (PipelineConfiguration pipeline: pipelines) {
            String containerName = format("%s-%s", runName, pipeline.sampleName()).toLowerCase();
            Container container = new Container();
            container.setImage("hartwigmedicalfoundation/pipeline5:platinum");
            container.setName(containerName);
            List<String> arguments = new ArrayList<>();
            arguments.add("/pipeline5.sh");
            for (Entry<String, String> entry : pipeline.arguments().entrySet()) {
                arguments.add(entry.getKey());
                arguments.add(entry.getValue());
            }
            container.setCommand(arguments);
            container.setVolumeMounts(List.of(new VolumeMountBuilder().withMountPath(SAMPLES_PATH).withName(configMapName).build(),
                    new VolumeMountBuilder().withMountPath(SECRETS_PATH).withName(jsonKeySecret).build()));

            String jobName = format("platinum-%s-%s", this.runName, pipeline.sampleName()).toLowerCase();
            JobSpecBuilder jobSpecBuilder = new JobSpecBuilder();
            jobSpecBuilder.withNewTemplate().withNewSpec()
                    .withContainers(container)
                    .withRestartPolicy("Never")
                    .withVolumes(List.of(
                            new VolumeBuilder().withName(configMapName).editOrNewConfigMap().withName(configMapName).endConfigMap().build(),
                            new VolumeBuilder().withName(jsonKeySecret).editOrNewSecret().withSecretName(jsonKeySecret).endSecret().build()))
                    .endSpec()
                    .endTemplate();
            kubernetesClient.batch().jobs().createNew().withNewMetadata().withName(jobName).endMetadata().withSpec(jobSpecBuilder.build()).done();
        }
    }

    public static KubernetesCluster findOrCreate(final String runName, final String configMapName, final String jsonSecretName,
            final String outputBucket, final KubernetesClient kubernetesClient) {
        try {
            return new KubernetesCluster(runName, configMapName, jsonSecretName, outputBucket, kubernetesClient);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create cluster", e);
        }
    }
}
