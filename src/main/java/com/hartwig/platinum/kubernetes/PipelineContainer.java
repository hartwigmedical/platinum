package com.hartwig.platinum.kubernetes;

import static java.lang.String.format;

import java.util.List;

import org.immutables.value.internal.$guava$.collect.$ClassToInstanceMap;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;

public class PipelineContainer implements KubernetesComponent<Container> {
    private final static String SAMPLES_PATH = "/samples";
    private final static String SECRETS_PATH = "/secrets";
    private final SampleArgument sample;
    private final String runName;
    private final PipelineArguments arguments;
    private final String serviceAccountKeySecretName;
    private final String configMapName;
    private final String imageName;

    public PipelineContainer(final SampleArgument sample, final String runName, final PipelineArguments arguments,
            final String serviceAccountKeySecretName, final String configMapName, final String imageName) {
        this.sample = sample;
        this.runName = runName;
        this.arguments = arguments;
        this.serviceAccountKeySecretName = serviceAccountKeySecretName;
        this.configMapName = configMapName;
        this.imageName = imageName;
    }

    @Override
    public Container asKubernetes() {
        String containerName = format("%s-%s", runName, sample.id()).toLowerCase();
        Container container = new Container();
        container.setEnv(List.of(new EnvVar("JAVA_OPTS",
                "-Djavax.net.ssl.keyStore=/jks/api.jks -Djavax.net.ssl.keyStorePassword=sbp123",
                null)));
        container.setImage(imageName);
        container.setName(containerName);
        List<String> command = arguments.asCommand(sample, SECRETS_PATH, serviceAccountKeySecretName);
        container.setCommand(command);
        container.setVolumeMounts(List.of(new VolumeMountBuilder().withMountPath(SAMPLES_PATH).withName(configMapName).build(),
                new VolumeMountBuilder().withMountPath(SECRETS_PATH).withName(serviceAccountKeySecretName).build(),
                new VolumeMountBuilder().withMountPath("/jks").withName("jks").build()));
        return container;
    }
}
