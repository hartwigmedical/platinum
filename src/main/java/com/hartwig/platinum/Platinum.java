package com.hartwig.platinum;

import static java.lang.String.format;

import java.io.File;

import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.iam.v1.Iam;
import com.google.cloud.storage.Storage;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.iam.PipelineIamPolicy;
import com.hartwig.platinum.iam.PipelineServiceAccount;
import com.hartwig.platinum.kubernetes.ConfigPopulator;
import com.hartwig.platinum.kubernetes.KubernetesCluster;
import com.hartwig.platinum.storage.OutputBucket;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

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

        String namespace = format("platinum-%s", runName);
        KubernetesClient client = new DefaultKubernetesClient();
        client.namespaces().createNew().withNewMetadata().withName(namespace).endMetadata().done();

        new ConfigPopulator(namespace, configuration).populateJson();
        KubernetesCluster cluster = KubernetesCluster.findOrCreate(runName, namespace,
                OutputBucket.from(storage).findOrCreate(runName, configuration.outputConfiguration()));
        cluster.submit(configuration);
        serviceAccount.delete(project, serviceAccountEmail);
    }
}
