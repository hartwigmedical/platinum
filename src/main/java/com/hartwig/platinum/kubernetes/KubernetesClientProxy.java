package com.hartwig.platinum.kubernetes;

import static java.util.List.of;

import java.util.Map;

import javax.inject.Provider;

import com.hartwig.platinum.config.GcpConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountList;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.ScalableResource;

public class KubernetesClientProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesClientProxy.class);
    private final String clusterName;
    private final String region;
    private final String project;
    private KubernetesClient kubernetesClient;

    public KubernetesClientProxy(String clusterName, GcpConfiguration gcpConfiguration, KubernetesClient kubernetesClient) {
        this.clusterName = clusterName;
        this.region = gcpConfiguration.regionOrThrow();
        this.project = gcpConfiguration.projectOrThrow();
        this.kubernetesClient = kubernetesClient;
        authorise();
    }

    public MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> configMaps() {
        return executeWithRetries(() -> kubernetesClient.configMaps());
    }

    public Volume ensureConfigMapVolumeExists(String volumeName, Map<String, String> configMapContents) {
        try {
            configMaps().inNamespace(KubernetesCluster.NAMESPACE)
                    .withName(volumeName)
                    .createOrReplace(new ConfigMapBuilder()
                            .addToData(configMapContents)
                            .withNewMetadata()
                            .withName(volumeName)
                            .withNamespace(KubernetesCluster.NAMESPACE)
                            .endMetadata()
                            .build());
            return new VolumeBuilder()
                    .withName(volumeName)
                    .editOrNewConfigMap()
                    .withName(volumeName)
                    .endConfigMap()
                    .build();
        } catch (KubernetesClientException e) {
            authorise();
            return ensureConfigMapVolumeExists(volumeName, configMapContents);
        }
    }

    public NonNamespaceOperation<Job, JobList, ScalableResource<Job>> jobs() {
        try {
            return kubernetesClient.batch().jobs().inNamespace(KubernetesCluster.NAMESPACE);
        } catch (KubernetesClientException e) {
            authorise();
            return jobs();
        }
    }

    private MixedOperation executeWithRetries(Provider<MixedOperation<?, ?, ?>> operationProvider) {
        try {
            return operationProvider.get();
        } catch (KubernetesClientException e) {
            authorise();
            return executeWithRetries(operationProvider);
        }
    }

    public MixedOperation<ServiceAccount, ServiceAccountList, Resource<ServiceAccount>> serviceAccounts() {
        return kubernetesClient.serviceAccounts();
    }

    public void authorise() {
        LOGGER.info("Authorising with cluster");
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
        kubernetesClient = new DefaultKubernetesClient();
    }
}
