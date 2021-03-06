package com.hartwig.platinum.kubernetes;

import com.hartwig.platinum.iam.JsonKey;

import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

public class PipelineServiceAccountSecretVolume implements KubernetesComponent<Volume> {
    private final JsonKey jsonKey;
    private final KubernetesClient kubernetesClient;
    private final String name;

    public PipelineServiceAccountSecretVolume(final JsonKey jsonKey, final KubernetesClient kubernetesClient, final String name) {
        this.jsonKey = jsonKey;
        this.kubernetesClient = kubernetesClient;
        this.name = name;
    }

    public Volume asKubernetes() {
        if (!jsonKey.secretExists()) {
            kubernetesClient.secrets()
                    .inNamespace(KubernetesCluster.NAMESPACE)
                    .withName(name)
                    .createOrReplace(new SecretBuilder().addToData(name, jsonKey.jsonBase64())
                            .withNewMetadata()
                            .withName(name)
                            .withNamespace(KubernetesCluster.NAMESPACE)
                            .endMetadata()
                            .build());
        }
        return new VolumeBuilder().withName(name).editOrNewSecret().withSecretName(name).endSecret().build();
    }
}
