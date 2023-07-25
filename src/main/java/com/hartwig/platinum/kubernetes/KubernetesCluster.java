package com.hartwig.platinum.kubernetes;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import com.hartwig.pdl.PipelineInput;
import com.hartwig.pdl.SampleInput;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.kubernetes.pipeline.PipelineArguments;
import com.hartwig.platinum.kubernetes.pipeline.PipelineConfigMapBuilder;
import com.hartwig.platinum.kubernetes.pipeline.PipelineContainer;
import com.hartwig.platinum.kubernetes.pipeline.PipelineJob;
import com.hartwig.platinum.kubernetes.pipeline.SampleArgument;
import com.hartwig.platinum.scheduling.JobScheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Volume;

public class KubernetesCluster {
    public final static String NAMESPACE = "default";
    private final static Logger LOGGER = LoggerFactory.getLogger(KubernetesCluster.class);
    private final String runName;
    private final JobScheduler scheduler;
    private final KubernetesComponent<Volume> serviceAccountSecret;
    private final List<Supplier<PipelineInput>> pipelineInputs;
    private final PipelineConfigMapBuilder configMaps;
    private final String outputBucketName;
    private final String serviceAccountEmail;
    private final PlatinumConfiguration configuration;
    private final TargetNodePool targetNodePool;

    KubernetesCluster(final String runName, final JobScheduler scheduler, final KubernetesComponent<Volume> serviceAccountSecret,
            final List<Supplier<PipelineInput>> pipelineInputs, final PipelineConfigMapBuilder configMaps, final String outputBucketName,
            final String serviceAccountEmail, final PlatinumConfiguration configuration, final TargetNodePool targetNodePool) {
        this.runName = runName.toLowerCase();
        this.scheduler = scheduler;
        this.serviceAccountSecret = serviceAccountSecret;
        this.pipelineInputs = pipelineInputs;
        this.configMaps = configMaps;
        this.outputBucketName = outputBucketName;
        this.serviceAccountEmail = serviceAccountEmail;
        this.configuration = configuration;
        this.targetNodePool = targetNodePool;
    }

    public int submit() {
        Volume secretVolume = serviceAccountSecret.asKubernetes();
        Volume maybeJksVolume = new JksSecret().asKubernetes();
        int numSubmitted = 0;
        for (Supplier<PipelineInput> inputSupplier : pipelineInputs) {
            PipelineInput pipelineInput = inputSupplier.get();
            String sampleName = pipelineInput.tumor().map(SampleInput::name).orElseThrow();
            SampleArgument sample = SampleArgument.sampleJson(sampleName, runName);
            Volume configMapVolume = configMaps.forSample(sampleName, pipelineInput);
            PipelineContainer pipelineContainer = new PipelineContainer(sample,
                    new PipelineArguments(configuration.argumentOverrides(), outputBucketName, serviceAccountEmail, configuration),
                    secretVolume.getName(),
                    configMapVolume.getName(),
                    configuration.image(),
                    configuration);
            scheduler.submit(new PipelineJob(sampleName + "-" + runName,
                    configuration.keystorePassword()
                            .map(p -> new JksEnabledContainer(pipelineContainer.asKubernetes(), maybeJksVolume, p).asKubernetes())
                            .orElse(pipelineContainer.asKubernetes()),
                    concat(of(configMapVolume, secretVolume), configuration.keystorePassword().map(p -> maybeJksVolume).stream()).collect(
                            toList()),
                    targetNodePool,
                    configuration.gcp().jobTtl().orElse(Duration.ZERO)));
            numSubmitted++;
        }
        return numSubmitted;
    }
}
