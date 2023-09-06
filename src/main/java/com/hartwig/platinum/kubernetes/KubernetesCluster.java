package com.hartwig.platinum.kubernetes;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import com.hartwig.pdl.PipelineInput;
import com.hartwig.pdl.SampleInput;
import com.hartwig.platinum.PlatinumResult;
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
    private final List<Supplier<PipelineInput>> pipelineInputs;
    private final PipelineConfigMapBuilder configMaps;
    private final String outputBucketName;
    private final PlatinumConfiguration configuration;
    private final TargetNodePool targetNodePool;

    KubernetesCluster(final String runName, final JobScheduler scheduler, final List<Supplier<PipelineInput>> pipelineInputs,
            final PipelineConfigMapBuilder configMaps, final String outputBucketName, final PlatinumConfiguration configuration,
            final TargetNodePool targetNodePool) {
        this.runName = runName.toLowerCase();
        this.scheduler = scheduler;
        this.pipelineInputs = pipelineInputs;
        this.configMaps = configMaps;
        this.outputBucketName = outputBucketName;
        this.configuration = configuration;
        this.targetNodePool = targetNodePool;
    }

    public PlatinumResult submit() {
        Volume maybeJksVolume = new JksSecret().asKubernetes();
        int numSubmitted = 0;
        int failures = 0;
        for (Supplier<PipelineInput> inputSupplier : pipelineInputs) {
            try {
                PipelineInput pipelineInput = inputSupplier.get();
                String sampleName = pipelineInput.tumor()
                        .or(pipelineInput::reference)
                        .map(SampleInput::name)
                        .orElseThrow(() -> new IllegalArgumentException("At least one tumor or reference sample must be provided"));
                SampleArgument sample = SampleArgument.sampleJson(sampleName, runName);
                Volume configMapVolume = configMaps.forSample(sampleName, pipelineInput);
                PipelineContainer pipelineContainer = new PipelineContainer(sample,
                        new PipelineArguments(configuration.argumentOverrides(),
                                outputBucketName,
                                configuration.serviceAccount().gcpEmailAddress(),
                                configuration),
                        configMapVolume.getName(),
                        configuration.image(),
                        configuration);

                scheduler.submit(new PipelineJob(sampleName + "-" + runName,
                        configuration.keystorePassword()
                                .map(p -> new JksEnabledContainer(pipelineContainer.asKubernetes(), maybeJksVolume, p).asKubernetes())
                                .orElse(pipelineContainer.asKubernetes()),
                        concat(of(configMapVolume), configuration.keystorePassword().map(p -> maybeJksVolume).stream()).collect(toList()),
                        configuration.serviceAccount().kubernetesServiceAccount(),
                        targetNodePool,
                        configuration.gcp().jobTtl().orElse(Duration.ZERO)));
                numSubmitted++;
            } catch (Exception e) {
                LOGGER.warn("Unexpected failure, skipping further processing of sample", e);
                failures++;
            }
        }
        return PlatinumResult.of(numSubmitted, failures);
    }
}
