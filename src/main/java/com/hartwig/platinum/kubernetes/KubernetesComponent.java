package com.hartwig.platinum.kubernetes;

public interface KubernetesComponent<T> {

    T asKubernetes();
}
