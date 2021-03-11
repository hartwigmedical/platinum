package com.hartwig.platinum.kubernetes;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

import java.util.ArrayList;
import java.util.List;

import com.hartwig.platinum.config.PlatinumConfiguration;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.batch.Job;

public class KubernetesCluster {

    private final String runName;
    private final JobScheduler scheduler;

    final static String NAMESPACE = "default";
    private final KubernetesComponent<Volume> serviceAccountSecret;
    private final KubernetesComponent<Volume> configMap;
    private final String outputBucketName;
    private final String serviceAccountEmail;
    private final PlatinumConfiguration configuration;

    KubernetesCluster(final String runName, final JobScheduler scheduler, final KubernetesComponent<Volume> serviceAccountSecret,
            final KubernetesComponent<Volume> configMap, final String outputBucketName, final String serviceAccountEmail,
            final PlatinumConfiguration configuration) {
        this.runName = runName.toLowerCase();
        this.scheduler = scheduler;
        this.serviceAccountSecret = serviceAccountSecret;
        this.configMap = configMap;
        this.outputBucketName = outputBucketName;
        this.serviceAccountEmail = serviceAccountEmail;
        this.configuration = configuration;
    }

    public List<Job> submit(final List<SampleArgument> samples) {
        Volume configMapVolume = configMap.asKubernetes();
        Volume secretVolume = serviceAccountSecret.asKubernetes();
        Volume maybeJksVolume = new JksSecret().asKubernetes();
        List<Job> submitted = new ArrayList<>();
        for (SampleArgument sample : samples) {
            PipelineContainer pipelineContainer = new PipelineContainer(sample,
                    runName,
                    new PipelineArguments(configuration.argumentOverrides(), outputBucketName, serviceAccountEmail, runName, configuration),
                    secretVolume.getName(),
                    configMapVolume.getName(),
                    configuration.image());
            submitted.add(scheduler.submit(new PipelineJob(runName,
                    sample.id(),
                    configuration.keystorePassword()
                            .map(p -> new JksEnabledContainer(pipelineContainer.asKubernetes(), maybeJksVolume, p).asKubernetes())
                            .orElse(pipelineContainer.asKubernetes()),
                    concat(of(configMapVolume, secretVolume), configuration.keystorePassword().map(p -> maybeJksVolume).stream()).collect(
                            toList()))));
        }
        return submitted;
    }
}
