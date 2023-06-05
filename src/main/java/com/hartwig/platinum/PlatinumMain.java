package com.hartwig.platinum;

import java.util.concurrent.Callable;

import javax.inject.Provider;

import com.google.cloud.storage.StorageOptions;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.config.Validation;
import com.hartwig.platinum.iam.IamProvider;
import com.hartwig.platinum.iam.ResourceManagerProvider;
import com.hartwig.platinum.kubernetes.ContainerProvider;
import com.hartwig.platinum.kubernetes.JobSubmitter;
import com.hartwig.platinum.kubernetes.KubernetesEngine;
import com.hartwig.platinum.kubernetes.ProcessRunner;
import com.hartwig.platinum.kubernetes.scheduling.JobScheduler;
import com.hartwig.platinum.pdl.PDLConversion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class PlatinumMain implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatinumMain.class);

    @Option(names = { "-n" },
            required = true,
            description = "The name of the run, used in output bucket and cluster naming")
    private String runName;

    @Option(names = { "-i" },
            required = true,
            description = "JSON or YAML file that contains arguments to be passed to the pipeline jobs and a list of samples")
    private String inputJson;

    @Option(names = { "-p" },
            description = "The GCP Project ID (not name!) where we'll run the pipelines.")
    private String project;

    @Option(names = { "-r" },
            description = "The GCP Region where we'll run the pipelines.")
    private String region;

    @Override
    public Integer call() {
        try {
            PlatinumConfiguration configuration = addRegionAndProject(PlatinumConfiguration.from(inputJson));
            Validation.apply(runName, configuration);

            Provider<KubernetesClient> kubernetesClientProvider = DefaultKubernetesClient::new;
            String clusterName = configuration.cluster().orElse(runName);
            JobSubmitter jobSubmitter = new JobSubmitter(clusterName, configuration.gcp(), configuration.retryFailed());
            JobScheduler jobScheduler = JobScheduler.fromConfiguration(configuration, jobSubmitter);

            new Platinum(runName,
                    inputJson,
                    StorageOptions.newBuilder().setProjectId(configuration.gcp().projectOrThrow()).build().getService(),
                    IamProvider.get(),
                    ResourceManagerProvider.get(),
                    new KubernetesEngine(ContainerProvider.get(), new ProcessRunner(), configuration, jobScheduler, kubernetesClientProvider),
                    configuration,
                    PDLConversion.create(configuration)).run();
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected exception", e);
            return 1;
        }
    }

    public PlatinumConfiguration addRegionAndProject(PlatinumConfiguration configuration) {
        configuration = addRegion(configuration);
        configuration = addProject(configuration);
        return configuration;
    }

    private PlatinumConfiguration addRegion(final PlatinumConfiguration configuration) {
        if (configuration.gcp().region().isPresent() && region != null) {
            throw new IllegalArgumentException("Region cannot be specified in both JSON input and CLI. Choose one or the other");
        } else if (region != null) {
            return configuration.withGcp(configuration.gcp().withRegion(region));
        }
        return configuration;
    }

    private PlatinumConfiguration addProject(final PlatinumConfiguration configuration) {
        if (configuration.gcp().project().isPresent() && project != null) {
            throw new IllegalArgumentException("Project cannot be specified in both JSON input and CLI. Choose one or the other");
        } else if (project != null) {
            return configuration.withGcp(configuration.gcp().withProject(project));
        }
        return configuration;
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new PlatinumMain()).execute(args));
    }
}
