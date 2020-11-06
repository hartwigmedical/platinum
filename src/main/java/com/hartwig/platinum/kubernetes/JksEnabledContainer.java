package com.hartwig.platinum.kubernetes;

import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;

public class JksEnabledContainer implements KubernetesComponent<Container> {

    private final Container wrapped;
    private final Volume jksVolume;
    private final String keystorePassword;

    public JksEnabledContainer(final Container wrapped, final Volume jksVolume, final String keystorePassword) {
        this.wrapped = wrapped;
        this.jksVolume = jksVolume;
        this.keystorePassword = keystorePassword;
    }

    @Override
    public Container asKubernetes() {
        return new ContainerBuilder(wrapped).withEnv(List.of(new EnvVar("JAVA_OPTS",
                String.format("-Djavax.net.ssl.keyStore=/jks/api.jks -Djavax.net.ssl.keyStorePassword=%s", keystorePassword),
                null))).addToVolumeMounts(new VolumeMountBuilder().withMountPath("/jks").withName(jksVolume.getName()).build()).build();
    }
}
