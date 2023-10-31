package com.hartwig.platinum;

import static com.hartwig.platinum.Console.bold;

import java.util.List;
import java.util.function.Supplier;

import com.google.cloud.storage.Storage;
import com.hartwig.pdl.PipelineInput;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.config.version.PipelineVersion;
import com.hartwig.platinum.config.version.VersionCompatibility;
import com.hartwig.platinum.kubernetes.KubernetesEngine;
import com.hartwig.platinum.pdl.PDLConversion;
import com.hartwig.platinum.storage.OutputBucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Platinum {

    private static final Logger LOGGER = LoggerFactory.getLogger(Platinum.class);
    private static final String MIN_PV5_VERSION = "5.33";

    private final String runName;
    private final String input;
    private final Storage storage;
    private final KubernetesEngine kubernetesEngine;
    private final PlatinumConfiguration configuration;
    private final PDLConversion pdlConversion;

    public Platinum(final String runName, final String input, final Storage storage, final KubernetesEngine clusterProvider,
            final PlatinumConfiguration configuration, final PDLConversion pdlConversion) {
        this.runName = runName;
        this.input = input;
        this.storage = storage;
        this.kubernetesEngine = clusterProvider;
        this.configuration = configuration;
        this.pdlConversion = pdlConversion;
    }

    public void run() {
        LOGGER.info("Starting Platinum run with name {} and input {}", bold(runName), bold(input));
        String clusterName = configuration.cluster().orElse(runName);

        new VersionCompatibility(MIN_PV5_VERSION, VersionCompatibility.UNLIMITED, new PipelineVersion()).check(configuration.image());

        List<Supplier<PipelineInput>> pipelineInputs = pdlConversion.apply(configuration);
        PlatinumResult result = kubernetesEngine.findOrCreate(clusterName,
                runName,
                pipelineInputs,
                OutputBucket.from(storage).findOrCreate(runName, configuration.gcp().regionOrThrow(), configuration)).submit();
        LOGGER.info("Platinum started {} pipelines on GCP", bold(String.valueOf(result.numSuccess())));
        if (result.numFailure() > 0) {
            LOGGER.info("{} pipelines failed on submission, see previous messages", bold(String.valueOf(result.numFailure())));
        }
        LOGGER.info("You can monitor their progress with: {}", bold("./platinum status"));
    }
}
