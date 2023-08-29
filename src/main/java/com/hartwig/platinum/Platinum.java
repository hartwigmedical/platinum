package com.hartwig.platinum;

import java.util.List;
import java.util.function.Supplier;

import com.google.cloud.storage.Storage;
import com.hartwig.pdl.PipelineInput;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.kubernetes.KubernetesEngine;
import com.hartwig.platinum.pdl.PDLConversion;
import com.hartwig.platinum.storage.OutputBucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Platinum {

    private static final Logger LOGGER = LoggerFactory.getLogger(Platinum.class);

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
        LOGGER.info("Starting Platinum run with name {} and input {}", Console.bold(runName), Console.bold(input));
        String clusterName = configuration.cluster().orElse(runName);

        List<Supplier<PipelineInput>> pipelineInputs = pdlConversion.apply(configuration);
        int submitted = kubernetesEngine.findOrCreate(clusterName,
                runName,
                pipelineInputs,
                OutputBucket.from(storage)
                        .findOrCreate(runName,
                                configuration.gcp().regionOrThrow(),
                                configuration)).submit();
        LOGGER.info("Platinum started {} pipelines on GCP", Console.bold(String.valueOf(submitted)));
        LOGGER.info("You can monitor their progress with: {}", Console.bold("./platinum status"));
    }

}
