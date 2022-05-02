package com.hartwig.platinum;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Objects;

import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.iam.v1.Iam;
import com.google.cloud.storage.Storage;
import com.hartwig.api.HmfApi;
import com.hartwig.platinum.config.GcpConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.config.SampleBucket;
import com.hartwig.platinum.iam.IamProvider;
import com.hartwig.platinum.iam.JsonKey;
import com.hartwig.platinum.iam.PipelineServiceAccount;
import com.hartwig.platinum.iam.ResourceManagerProvider;
import com.hartwig.platinum.iam.ServiceAccountPrivateKey;
import com.hartwig.platinum.kubernetes.ContainerProvider;
import com.hartwig.platinum.kubernetes.KubernetesEngine;
import com.hartwig.platinum.kubernetes.ProcessRunner;
import com.hartwig.platinum.kubernetes.SampleArgument;
import com.hartwig.platinum.p5sample.DecomposeSamples;
import com.hartwig.platinum.p5sample.TumorNormalPair;
import com.hartwig.platinum.storage.OutputBucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Platinum {

    private static final Logger LOGGER = LoggerFactory.getLogger(Platinum.class);

    private final String runName;
    private final String input;
    private final Storage storage;
    private final Iam iam;
    private final CloudResourceManager resourceManager;
    private final KubernetesEngine kubernetesEngine;
    private final PlatinumConfiguration configuration;
    private final ApiRerun apiRerun;

    public Platinum(final String runName, final String input, final Storage storage, final Iam iam,
            final CloudResourceManager resourceManager, final KubernetesEngine clusterProvider, final PlatinumConfiguration configuration,
            final ApiRerun apiRerun) {
        this.runName = runName;
        this.input = input;
        this.storage = storage;
        this.iam = iam;
        this.resourceManager = resourceManager;
        this.kubernetesEngine = clusterProvider;
        this.configuration = configuration;
        this.apiRerun = apiRerun;
    }

    public static Platinum create(final String runName, final PlatinumConfiguration configuration, final String input,
            final Storage storage) {
        final HmfApi api = HmfApi.create(HmfApi.PRODUCTION);
        return new Platinum(runName,
                input,
                storage,
                IamProvider.get(),
                ResourceManagerProvider.get(),
                new KubernetesEngine(ContainerProvider.get(), new ProcessRunner(), configuration),
                configuration,
                new ApiRerun(api.runs(), api.sets(), api.samples(), configuration.outputBucket().get(), "5.27.1"));
    }

    public void run() {
        LOGGER.info("Starting platinum run with name {} and input {}", Console.bold(runName), Console.bold(input));
        GcpConfiguration gcpConfiguration = configuration.gcp();
        PipelineServiceAccount serviceAccount =
                PipelineServiceAccount.from(iam, resourceManager, runName, gcpConfiguration.projectOrThrow(), configuration);
        String serviceAccountEmail = serviceAccount.findOrCreate();
        ServiceAccountPrivateKey privateKey = ServiceAccountPrivateKey.from(configuration, iam);
        JsonKey jsonKey = privateKey.create(gcpConfiguration.projectOrThrow(), serviceAccountEmail);
        List<TumorNormalPair> pairs = DecomposeSamples.apply(configuration.sampleBucket()
                .map(b -> new SampleBucket(storage.get(b)).apply())
                .orElseGet(configuration::samples));
        int submitted = kubernetesEngine.findOrCreate(runName,
                pairs,
                jsonKey,
                OutputBucket.from(storage).findOrCreate(runName, gcpConfiguration.regionOrThrow(), serviceAccountEmail, configuration),
                serviceAccountEmail).submit(samples(configuration, pairs, runName));
        LOGGER.info("Platinum started {} pipelines on GCP", Console.bold(String.valueOf(submitted)));
        LOGGER.info("You can monitor their progress with: {}", Console.bold("./platinum status"));
    }

    private List<SampleArgument> samples(final PlatinumConfiguration configuration, final List<TumorNormalPair> pairs,
            final String runName) {
        if (configuration.sampleIds().isEmpty()) {
            return pairs.stream().map(p -> SampleArgument.sampleJson(p, runName)).collect(toList());
        } else {
            if (configuration.apiUrl().isPresent()) {
                return configuration.sampleIds().parallelStream().map(s -> {
                    final Long runId = apiRerun.create(s);
                    if (runId != null) {
                        return SampleArgument.runId(s, runId);
                    }
                    return null;
                }).filter(Objects::nonNull).collect(toList());
            } else {
                return configuration.sampleIds().stream().map(s -> SampleArgument.biopsy(s, runName)).collect(toList());
            }
        }
    }

}
