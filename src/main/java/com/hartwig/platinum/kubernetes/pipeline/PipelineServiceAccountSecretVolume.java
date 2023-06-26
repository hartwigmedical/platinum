package com.hartwig.platinum.kubernetes.pipeline;

import com.hartwig.platinum.iam.JsonKey;
import com.hartwig.platinum.kubernetes.KubernetesClientProxy;
import com.hartwig.platinum.kubernetes.KubernetesCluster;
import com.hartwig.platinum.kubernetes.KubernetesComponent;

import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;

public class PipelineServiceAccountSecretVolume implements KubernetesComponent<Volume> {
    private final JsonKey jsonKey;
    private final KubernetesClientProxy kubernetesClientProxy;
    private final String name;

    public PipelineServiceAccountSecretVolume(final JsonKey jsonKey, final KubernetesClientProxy kubernetesClientProxy, final String name) {
        this.jsonKey = jsonKey;
        this.kubernetesClientProxy = kubernetesClientProxy;
        this.name = name;
    }

    public Volume asKubernetes() {
        if (!jsonKey.secretExists()) {
            try {
                kubernetesClientProxy.secrets()
                        .inNamespace(KubernetesCluster.NAMESPACE)
                        .withName(name)
                        .createOrReplace(new SecretBuilder().addToData(name, jsonKey.jsonBase64())
                                .withNewMetadata()
                                .withName(name)
                                .withNamespace(KubernetesCluster.NAMESPACE)
                                .endMetadata()
                                .build());
            } catch (KubernetesClientException e) {
                kubernetesClientProxy.authorise();
                return asKubernetes();
            }
        }
        return new VolumeBuilder().withName(name).editOrNewSecret().withSecretName(name).endSecret().build();
    }
}