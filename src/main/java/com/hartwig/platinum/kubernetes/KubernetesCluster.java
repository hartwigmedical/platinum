package com.hartwig.platinum.kubernetes;

import com.hartwig.platinum.config.RunConfiguration;
import com.hartwig.platinum.storage.OutputBucket;

import io.kubernetes.client.openapi.apis.CoreV1Api;

public class KubernetesCluster {

    private final CoreV1Api api;

    private KubernetesCluster(final CoreV1Api api) {
        this.api = api;
    }

    public PipelineFutures submit(final RunConfiguration configuration) {
        return new PipelineFutures();
    }

    public static KubernetesCluster findOrCreate(final String runName, final String outputBucketName) {
        // we should have some kind of convention on cluster name based on run name
        return new KubernetesCluster(new CoreV1Api());
    }
}
