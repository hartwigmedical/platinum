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
import com.hartwig.platinum.kubernetes.KubernetesCluster;
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
    private final String project;
    private final String region;

    public Platinum(final String runName, final String input, final Storage storage, final Iam iam,
            final CloudResourceManager resourceManager, final String project, final String region) {
        this.runName = runName;
        this.input = input;
        this.storage = storage;
        this.iam = iam;
        this.resourceManager = resourceManager;
        this.project = project;
        this.region = region;
    }

    public void run() {
        LOGGER.info("Starting platinum run with name [{}] and input [{}]", runName, input);
        PlatinumConfiguration configuration = PlatinumConfiguration.from(new File(input));
        PipelineServiceAccount serviceAccount = new PipelineServiceAccount(iam, new PipelineIamPolicy(resourceManager));
        String serviceAccountEmail = serviceAccount.findOrCreate(project, runName);

        ServiceAccountPrivateKey privateKey = new ServiceAccountPrivateKey(iam);
        JsonKey jsonKey = privateKey.create(project, serviceAccountEmail);
        KubernetesCluster.findOrCreate(runName, project, region)
                .submit(configuration,
                        jsonKey,
                        OutputBucket.from(storage).findOrCreate(runName, region, serviceAccountEmail, configuration),
                        project,
                        region,
                        serviceAccountEmail);
        LOGGER.info("[{}] pipelines started on GCP", configuration.samples().size());
        LOGGER.info("You can monitor their progress with: \nkubectl get pods | grep {}", runName);
    }
}
