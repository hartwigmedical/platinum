package com.hartwig.platinum.kubernetes;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

import java.time.Duration;
import java.util.List;

import com.hartwig.platinum.config.BatchConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.client.KubernetesClientException;

public class KubernetesCluster {

    private final static Logger LOGGER = LoggerFactory.getLogger(KubernetesCluster.class);

    private final String runName;
    private final JobScheduler scheduler;

    final static String NAMESPACE = "default";
    private final KubernetesComponent<Volume> serviceAccountSecret;
    private final KubernetesComponent<Volume> configMap;
    private final String outputBucketName;
    private final String serviceAccountEmail;
    private final PlatinumConfiguration configuration;
    private final Delay delay;
    private final TargetNodePool targetNodePool;

    KubernetesCluster(final String runName, final JobScheduler scheduler, final KubernetesComponent<Volume> serviceAccountSecret,
            final KubernetesComponent<Volume> configMap, final String outputBucketName, final String serviceAccountEmail,
            final PlatinumConfiguration configuration, final Delay delay, final TargetNodePool targetNodePool) {
        this.runName = runName.toLowerCase();
        this.scheduler = scheduler;
        this.serviceAccountSecret = serviceAccountSecret;
        this.configMap = configMap;
        this.outputBucketName = outputBucketName;
        this.serviceAccountEmail = serviceAccountEmail;
        this.configuration = configuration;
        this.delay = delay;
        this.targetNodePool = targetNodePool;
    }

    public int submit(final List<SampleArgument> samples) {
        Volume configMapVolume = configMap.asKubernetes();
        Volume secretVolume = serviceAccountSecret.asKubernetes();
        Volume maybeJksVolume = new JksSecret().asKubernetes();
        int numSubmitted = 0;
        for (SampleArgument sample : samples) {
            try {
                PipelineContainer pipelineContainer = new PipelineContainer(sample,
                        runName,
                        new PipelineArguments(configuration.argumentOverrides(),
                                outputBucketName,
                                serviceAccountEmail, configuration),
                        secretVolume.getName(),
                        configMapVolume.getName(),
                        configuration.image(), configuration);
                if (scheduler.submit(new PipelineJob(runName,
                        sample.id(),
                        configuration.keystorePassword()
                                .map(p -> new JksEnabledContainer(pipelineContainer.asKubernetes(), maybeJksVolume, p).asKubernetes())
                                .orElse(pipelineContainer.asKubernetes()),
                        concat(of(configMapVolume, secretVolume),
                                configuration.keystorePassword().map(p -> maybeJksVolume).stream()).collect(toList()),
                        targetNodePool,
                        configuration.gcp().jobTtl().orElse(Duration.ZERO)))) {
                    numSubmitted++;
                    if (configuration.batch().isPresent()) {
                        BatchConfiguration batchConfiguration = configuration.batch().get();
                        if (numSubmitted % batchConfiguration.size() == 0) {
                            LOGGER.info("Batch of [{}] of [{}] scheduled, waiting [{}] minutes",
                                    (numSubmitted / batchConfiguration.size()),
                                    (samples.size() / batchConfiguration.size()),
                                    batchConfiguration.delay());
                            delay.forMinutes(batchConfiguration.delay());
                        }
                    }
                }
            } catch (KubernetesClientException e) {
                LOGGER.warn("Refreshing K8 client as an error was encountered. ", e);
                scheduler.refresh(configuration.cluster().orElse(runName), configuration.gcp());
            }
        }
        return numSubmitted;
    }
}
