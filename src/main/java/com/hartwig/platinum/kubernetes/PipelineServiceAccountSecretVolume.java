package com.hartwig.platinum.kubernetes;

import com.hartwig.platinum.iam.JsonKey;

import org.immutables.value.internal.$processor$.meta.$FactoryMirror;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

public class PipelineServiceAccountSecretVolume {
    private final JsonKey jsonKey;
    private final KubernetesClient kubernetesClient;

    public PipelineServiceAccountSecretVolume(final JsonKey jsonKey, final KubernetesClient kubernetesClient) {
        this.jsonKey = jsonKey;
        this.kubernetesClient = kubernetesClient;
    }

    public Volume create(final String name) {
        kubernetesClient.secrets()
                .createNew()
                .withNewMetadata()
                .withName(name)
                .withNamespace(KubernetesCluster.NAMESPACE)
                .endMetadata()
                .addToData(name, jsonKey.jsonBase64())
                .done();
        return new VolumeBuilder().withName(name).editOrNewSecret().withSecretName(name).endSecret().build();
    }
}
