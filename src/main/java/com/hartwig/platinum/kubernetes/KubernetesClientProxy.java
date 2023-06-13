package com.hartwig.platinum.kubernetes;

import static java.util.List.of;

import com.hartwig.platinum.config.GcpConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.ScalableResource;

public class KubernetesClientProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesClientProxy.class);
    private final String clusterName;
    private final String region;
    private final String project;
    private final KubernetesClient kubernetesClient;

    public KubernetesClientProxy(String clusterName, GcpConfiguration gcpConfiguration,
            KubernetesClient kubernetesClient) {
        this.clusterName = clusterName;
        this.region = gcpConfiguration.regionOrThrow();
        this.project = gcpConfiguration.projectOrThrow();
        this.kubernetesClient = kubernetesClient;
    }

    public MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> configMaps() {
        return kubernetesClient.configMaps();
    }

    public MixedOperation<Secret, SecretList, Resource<Secret>> secrets() {
        return kubernetesClient.secrets();
    }

    public NonNamespaceOperation<Job, JobList, ScalableResource<Job>> jobs() {
        return kubernetesClient.batch().jobs().inNamespace(KubernetesCluster.NAMESPACE);
    }

    public void reAuthorise() {
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
        if (!processRunner.execute(of("kubectl", "get", "nodes"))) {
            throw new RuntimeException("Failed to run kubectl command against cluster");
        }
    }
}
