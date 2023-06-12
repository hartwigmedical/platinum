package com.hartwig.platinum.kubernetes;

import static java.util.List.of;

import javax.inject.Provider;

import com.hartwig.platinum.config.GcpConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class KubernetesClientProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesClientProxy.class);
    private final String clusterName;
    private final String region;
    private final String project;
    private Provider<KubernetesClient> kubernetesClientProvider;

    public KubernetesClientProxy(String clusterName, GcpConfiguration gcpConfiguration,
            Provider<KubernetesClient> kubernetesClientProvider) {
        this.clusterName = clusterName;
        this.region = gcpConfiguration.regionOrThrow();
        this.project = gcpConfiguration.projectOrThrow();
        this.kubernetesClientProvider = kubernetesClientProvider;
    }

    protected MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> configMaps() {
        return kubernetesClientProvider.get().configMaps();
    }

    protected MixedOperation<Secret, SecretList, Resource<Secret>> secrets() {
        return kubernetesClientProvider.get().secrets();
    }

    protected void reAuthorise() {
        LOGGER.info("Re-authorising with cluster");
        ProcessRunner processRunner = new ProcessRunner();
        if (!processRunner.execute(of("gcloud",
                "container",
                "clusters",
                "get-credentials",
                clusterName,
                "--region",
                region,
                "--project",
                project))) {
            throw new RuntimeException("Failed to get credentials for cluster");
        }
        if (!processRunner.execute(of("kubectl", "get", "configmaps"))) {
            throw new RuntimeException("Failed to run kubectl command against cluster");
        }
    }
}
