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

public class Platinum {

    private final String runName;
    private final String input;
    private final Storage storage;
    private final Iam iam;
    private final CloudResourceManager resourceManager;
    private final String project;

    public Platinum(final String runName, final String input, final Storage storage, final Iam iam,
            final CloudResourceManager resourceManager, final String project) {
        this.runName = runName;
        this.input = input;
        this.storage = storage;
        this.iam = iam;
        this.resourceManager = resourceManager;
        this.project = project;
    }

    public void run() {
        PlatinumConfiguration configuration = PlatinumConfiguration.from(new File(input));
        PipelineServiceAccount serviceAccount = new PipelineServiceAccount(iam, new PipelineIamPolicy(resourceManager));
        String serviceAccountEmail = serviceAccount.findOrCreate(project, runName);
        ServiceAccountPrivateKey privateKey = new ServiceAccountPrivateKey(iam);
        JsonKey keyJson = privateKey.create(project, serviceAccountEmail);

        KubernetesCluster cluster = KubernetesCluster.findOrCreate(runName,
                OutputBucket.from(storage).findOrCreate(runName, configuration.outputConfiguration()));
        cluster.submit(configuration);

        privateKey.delete(keyJson);
        serviceAccount.delete(project, serviceAccountEmail);
    }
}
