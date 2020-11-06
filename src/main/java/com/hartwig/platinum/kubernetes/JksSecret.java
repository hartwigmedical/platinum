package com.hartwig.platinum.kubernetes;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;

public class JksSecret implements KubernetesComponent<Volume> {
    @Override
    public Volume asKubernetes() {
        return new VolumeBuilder().withName("jks").editOrNewSecret().withSecretName("jks").endSecret().build();
    }
}
