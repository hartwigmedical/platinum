package com.hartwig.platinum.kubernetes;

import static java.lang.String.format;

import com.hartwig.platinum.config.PlatinumConfiguration;

import io.kubernetes.client.openapi.apis.CoreV1Api;

public class KubernetesCluster {
    private final String runName;
    private final String name;
    private final String namespace;
    private final CoreV1Api api;

    private KubernetesCluster(final String runName, final CoreV1Api api) {
        this.runName = runName;
        this.name = format("platinum-%s-cluster", runName);
        this.namespace = "platinum-" + runName;
        this.api = api;
    }

    public void submit(final PlatinumConfiguration configuration) {

    }

    public static KubernetesCluster findOrCreate(final String runName, final String outputBucket) {
        return new KubernetesCluster(runName, new CoreV1Api());
    }
}
