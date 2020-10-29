package com.hartwig.platinum;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import com.google.cloud.storage.StorageOptions;
import com.hartwig.platinum.iam.IamProvider;
import com.hartwig.platinum.iam.ResourceManagerProvider;
import com.hartwig.platinum.kubernetes.ContainerProvider;
import com.hartwig.platinum.kubernetes.KubernetesEngine;
import com.hartwig.platinum.kubernetes.ProcessRunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            description = "JSON file that contains arguments to be passed to the pipeline jobs and a list of samples")
    private String inputJson;

    @Option(names = { "-p" },
            required = true,
            description = "The GCP Project ID (not name!) where we'll run the pipelines.")
    private String project;

    @Option(names = { "-r" },
            required = true,
            description = "The GCP Region where we'll run the pipelines.")
    private String region;

    @Option(names = { "--network" },
            defaultValue = "default",
            description = "The network in which all compute resources should be created (pipeline and GKE)")
    private String network;

    @Option(names = { "--subnet" },
            defaultValue = "default",
            description = "The subnet in which all compute resources should be created (pipeline and GKE)")
    private String subnet;

    @Option(names = { "--network_tags" },
            split = ",",
            description = "Comma delimited list of network tags to be applied to GKE and pipeline resources")
    private List<String> networkTags;

    @Option(names = { "--private_cluster" },
            description = "When specified platinum will create a private K8 cluster with no public internet access. "
                    + "It will only be accessible from instances within the VPC network.")
    private boolean privateCluster;

    @Option(names = { "--zones" },
            split = ",",
            description = "A list of zones within the region in which to make cluster nodes.")
    private List<String> zones;

    @Option(names = { "--secondary_range_name_pods" },
            description = "The secondary range name used by pods when launching the cluster in a shared VPC setup.")
    private String secondaryRangeNamePods;

    @Option(names = { "--secondary_range_name_services" },
            description = "The secondary range name used by services when launching the cluster in a shared VPC setup.")
    private String secondaryRangeNameServices;


    @Override
    public Integer call() {
        try {
            new Platinum(runName,
                    inputJson,
                    StorageOptions.newBuilder().setProjectId(project).build().getService(),
                    IamProvider.get(),
                    ResourceManagerProvider.get(),
                    new KubernetesEngine(ContainerProvider.get(), new ProcessRunner()),
                    GcpConfiguration.builder()
                            .project(project)
                            .region(region)
                            .network(network)
                            .subnet(subnet)
                            .networkTags(networkTags != null ? networkTags : Collections.emptyList())
                            .privateCluster(privateCluster)
                            .secondaryRangeNamePods(Optional.ofNullable(secondaryRangeNamePods))
                            .secondaryRangeNameServices(Optional.ofNullable(secondaryRangeNameServices))
                            .zones(zones != null ? zones : Collections.emptyList())
                            .build()).run();
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected exception", e);
            return 1;
        }
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new PlatinumMain()).execute(args));
    }
}
