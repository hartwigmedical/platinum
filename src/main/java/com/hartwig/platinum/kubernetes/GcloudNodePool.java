package com.hartwig.platinum.kubernetes;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GcloudNodePool {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcloudNodePool.class);

    private final ProcessRunner processRunner;

    public GcloudNodePool(final ProcessRunner processRunner) {
        this.processRunner = processRunner;
    }

    void create(final TargetNodePool targetNodePool, final String serviceAccount, final String clusterName, final String project) {
        LOGGER.info("Creating new node pool with name [{}] machine type [{}] and [{}] nodes",
                targetNodePool.name(),
                targetNodePool.machineType(),
                targetNodePool.numNodes());
        processRunner.execute(List.of("gcloud",
                "container",
                "node-pools",
                "create",
                targetNodePool.name(),
                "--cluster=" + clusterName,
                "--machine-type=" + targetNodePool.machineType(),
                "--node-labels=pool=" + targetNodePool.name(),
                "--node-taints=reserved-pool=true:NoSchedule",
                "--num-nodes=" + targetNodePool.numNodes(),
                "--region=europe-west4",
                "--project=" + project,
                "--service-account=" + serviceAccount));
    }
}
