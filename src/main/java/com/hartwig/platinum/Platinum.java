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
import com.hartwig.platinum.kubernetes.PipelineConfigMapVolume;
import com.hartwig.platinum.kubernetes.PipelineServiceAccountSecretVolume;
import com.hartwig.platinum.storage.OutputBucket;

import io.fabric8.kubernetes.client.KubernetesClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Platinum {

    private static final Logger LOGGER = LoggerFactory.getLogger(Platinum.class);

    private final String runName;
    private final String input;
    private final Storage storage;
    private final Iam iam;
    private final CloudResourceManager resourceManager;
    private final KubernetesClient kubernetesClient;
    private final String project;

    public Platinum(final String runName, final String input, final Storage storage, final Iam iam,
            final CloudResourceManager resourceManager, final KubernetesClient kubernetesClient, final String project) {
        this.runName = runName;
        this.input = input;
        this.storage = storage;
        this.iam = iam;
        this.resourceManager = resourceManager;
        this.kubernetesClient = kubernetesClient;
        this.project = project;
    }

    public void run() {

        LOGGER.info("Starting platinum run with name [{}] and input [{}]", runName, input);
        PlatinumConfiguration configuration = PlatinumConfiguration.from(new File(input));
        PipelineServiceAccount serviceAccount = new PipelineServiceAccount(iam, new PipelineIamPolicy(resourceManager));
        String serviceAccountEmail = serviceAccount.findOrCreate(project, runName);
        LOGGER.info("Created service account with email [{}].", serviceAccountEmail);
        ServiceAccountPrivateKey privateKey = new ServiceAccountPrivateKey(iam);
        JsonKey jsonKey = privateKey.create(project, serviceAccountEmail);
        LOGGER.info("Created private key for service account [{}] with id [{}]", serviceAccountEmail, jsonKey.id());
        KubernetesCluster cluster = KubernetesCluster.findOrCreate(runName,
                OutputBucket.from(storage).findOrCreate(runName, configuration.outputConfiguration()),
                kubernetesClient);
        cluster.submit(configuration, jsonKey);
        LOGGER.info("Submitted workloads to kubernetes");
    }
}
