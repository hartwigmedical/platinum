package com.hartwig.platinum;

import java.io.File;

import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.iam.v1.Iam;
import com.google.cloud.storage.Storage;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.iam.JsonKey;
import com.hartwig.platinum.iam.PipelineIamPolicy;
import com.hartwig.platinum.iam.PipelineServiceAccount;
import com.hartwig.platinum.iam.ServiceAccountPrivateKey;
import com.hartwig.platinum.kubernetes.KubernetesClusterProvider;
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
    private final KubernetesClusterProvider clusterProvider;
    private final GcpConfiguration gcpConfiguration;

    public Platinum(final String runName, final String input, final Storage storage, final Iam iam,
            final CloudResourceManager resourceManager, final KubernetesClusterProvider clusterProvider,
            final GcpConfiguration gcpConfiguration) {
        this.runName = runName;
        this.input = input;
        this.storage = storage;
        this.iam = iam;
        this.resourceManager = resourceManager;
        this.clusterProvider = clusterProvider;
        this.gcpConfiguration = gcpConfiguration;
    }

    public void run() {
        LOGGER.info("Starting platinum run with name {} and input {}", Console.bold(runName), Console.bold(input));
        PlatinumConfiguration configuration = PlatinumConfiguration.from(new File(input));
        PipelineServiceAccount serviceAccount = new PipelineServiceAccount(iam, new PipelineIamPolicy(resourceManager));
        String serviceAccountEmail = serviceAccount.findOrCreate(gcpConfiguration.project(), runName);
        ServiceAccountPrivateKey privateKey = new ServiceAccountPrivateKey(iam);
        JsonKey jsonKey = privateKey.create(gcpConfiguration.project(), serviceAccountEmail);
        clusterProvider.findOrCreate(runName, gcpConfiguration)
                .submit(configuration,
                        jsonKey,
                        OutputBucket.from(storage).findOrCreate(runName, gcpConfiguration.region(), serviceAccountEmail, configuration),
                        gcpConfiguration,
                        serviceAccountEmail);
        LOGGER.info("Platinum started [{}] pipelines on GCP", configuration.samples().size());
        LOGGER.info("You can monitor their progress with: {}", Console.bold("./platinum status"));
    }
}
