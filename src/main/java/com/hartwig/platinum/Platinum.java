package com.hartwig.platinum;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Set;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.container.v1beta1.Container;
import com.google.api.services.container.v1beta1.ContainerScopes;
import com.google.api.services.iam.v1.Iam;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.iam.JsonKey;
import com.hartwig.platinum.iam.PipelineIamPolicy;
import com.hartwig.platinum.iam.PipelineServiceAccount;
import com.hartwig.platinum.iam.ServiceAccountPrivateKey;
import com.hartwig.platinum.kubernetes.KubernetesClusterProvider;
import com.hartwig.platinum.kubernetes.ProcessRunner;
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
        LOGGER.info("Starting platinum run with name {} and input {}", Console.bold(runName), Console.bold(input));
        PlatinumConfiguration configuration = PlatinumConfiguration.from(new File(input));
        PipelineServiceAccount serviceAccount = new PipelineServiceAccount(iam, new PipelineIamPolicy(resourceManager));
        String serviceAccountEmail = serviceAccount.findOrCreate(project, runName);

        ServiceAccountPrivateKey privateKey = new ServiceAccountPrivateKey(iam);
        JsonKey jsonKey = privateKey.create(project, serviceAccountEmail);

        try {
            Container container = new Container.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(GoogleCredentials.getApplicationDefault().createScoped(Set.of(ContainerScopes.CLOUD_PLATFORM)))).setApplicationName("platinum").build();
            new KubernetesClusterProvider(container, new ProcessRunner()).findOrCreate(runName, project, region)
                    .submit(configuration,
                            jsonKey,
                            OutputBucket.from(storage).findOrCreate(runName, region, serviceAccountEmail, configuration),
                            project,
                            region,
                            serviceAccountEmail);
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("Platinum started [{}] pipelines on GCP", configuration.samples().size());
        LOGGER.info("You can monitor their progress with: {}", Console.bold("./platinum status"));
    }
}
