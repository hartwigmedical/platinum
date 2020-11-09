package com.hartwig.platinum;

import java.util.List;

import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.iam.v1.Iam;
import com.google.cloud.storage.Storage;
import com.hartwig.platinum.config.GcpConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.iam.JsonKey;
import com.hartwig.platinum.iam.PipelineServiceAccount;
import com.hartwig.platinum.iam.ServiceAccountPrivateKey;
import com.hartwig.platinum.kubernetes.KubernetesEngine;
import com.hartwig.platinum.storage.OutputBucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.batch.Job;

public class Platinum {

    private static final Logger LOGGER = LoggerFactory.getLogger(Platinum.class);

    private final String runName;
    private final String input;
    private final Storage storage;
    private final Iam iam;
    private final CloudResourceManager resourceManager;
    private final KubernetesEngine kubernetesEngine;
    private PlatinumConfiguration configuration;

    public Platinum(final String runName, final String input, final Storage storage, final Iam iam,
            final CloudResourceManager resourceManager, final KubernetesEngine clusterProvider, final PlatinumConfiguration configuration) {
        this.runName = runName;
        this.input = input;
        this.storage = storage;
        this.iam = iam;
        this.resourceManager = resourceManager;
        this.kubernetesEngine = clusterProvider;
        this.configuration = configuration;
    }

    public void run() {
        LOGGER.info("Starting platinum run with name {} and input {}", Console.bold(runName), Console.bold(input));
        GcpConfiguration gcpConfiguration = configuration.gcp();
        PipelineServiceAccount serviceAccount =
                PipelineServiceAccount.from(iam, resourceManager, runName, gcpConfiguration.projectOrThrow(), configuration);
        String serviceAccountEmail = serviceAccount.findOrCreate();
        ServiceAccountPrivateKey privateKey = ServiceAccountPrivateKey.from(configuration, iam);
        JsonKey jsonKey = privateKey.create(gcpConfiguration.projectOrThrow(), serviceAccountEmail);
        List<Job> submitted = kubernetesEngine.findOrCreate(runName,
                configuration,
                jsonKey,
                OutputBucket.from(storage).findOrCreate(runName, gcpConfiguration.regionOrThrow(), serviceAccountEmail, configuration),
                serviceAccountEmail).submit(configuration);
        LOGGER.info("Platinum started {} pipelines on GCP", Console.bold(String.valueOf(submitted.size())));
        LOGGER.info("You can monitor their progress with: {}", Console.bold("./platinum status"));
    }
}
