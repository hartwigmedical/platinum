package com.hartwig.platinum.kubernetes;

import com.hartwig.platinum.iam.JsonKey;

import io.fabric8.kubernetes.client.KubernetesClient;

public class PipelineServiceAccountSecret {
    private JsonKey jsonKey;
    private KubernetesClient kubernetesClient;

    public PipelineServiceAccountSecret(JsonKey jsonKey, KubernetesClient kubernetesClient) {
        this.jsonKey = jsonKey;
        this.kubernetesClient = kubernetesClient;
    }

    public String create() {
        String keyName = "compute-key";
        kubernetesClient.secrets().createNew().withNewMetadata().withName(keyName).withNamespace(KubernetesCluster.NAMESPACE).endMetadata().addToData(keyName, jsonKey.jsonBase64()).done();
        return keyName;
    }
}
