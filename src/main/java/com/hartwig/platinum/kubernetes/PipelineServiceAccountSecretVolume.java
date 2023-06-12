package com.hartwig.platinum.kubernetes;

import com.hartwig.platinum.iam.JsonKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;

public class PipelineServiceAccountSecretVolume implements KubernetesComponent<Volume> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineServiceAccountSecretVolume.class);
    private final JsonKey jsonKey;
    private final KubernetesClientProxy kubernetesClient;
    private final String name;

    public PipelineServiceAccountSecretVolume(final JsonKey jsonKey, final KubernetesClientProxy kubernetesClient, final String name) {
        this.jsonKey = jsonKey;
        this.kubernetesClient = kubernetesClient;
        this.name = name;
    }

    public Volume asKubernetes() {
        if (!jsonKey.secretExists()) {
            try {
                kubernetesClient.secrets()
                        .inNamespace(KubernetesCluster.NAMESPACE)
                        .withName(name)
                        .createOrReplace(new SecretBuilder().addToData(name, jsonKey.jsonBase64())
                                .withNewMetadata()
                                .withName(name)
                                .withNamespace(KubernetesCluster.NAMESPACE)
                                .endMetadata()
                                .build());
            } catch (KubernetesClientException e) {
                kubernetesClient.reAuthorise();
                return asKubernetes();
            }
        }
        return new VolumeBuilder().withName(name).editOrNewSecret().withSecretName(name).endSecret().build();
    }
}
