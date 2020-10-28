package com.hartwig.platinum.kubernetes;

import static java.lang.String.format;

import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;

public class PipelineContainer implements KubernetesComponent<Container> {
    private final static String SAMPLES_PATH = "/samples";
    private final static String SECRETS_PATH = "/secrets";
    private static final String IMAGE = "eu.gcr.io/hmf-images/pipeline5:platinum";
    private final SampleArgument sample;
    private final String runName;
    private final PipelineArguments arguments;
    private final String serviceAccountKeySecretName;
    private final String configMapName;

    public PipelineContainer(final SampleArgument sample, final String runName, final PipelineArguments arguments,
            final String serviceAccountKeySecretName, final String configMapName) {
        this.sample = sample;
        this.runName = runName;
        this.arguments = arguments;
        this.serviceAccountKeySecretName = serviceAccountKeySecretName;
        this.configMapName = configMapName;
    }

    @Override
    public Container asKubernetes() {
        String containerName = format("%s-%s", runName, sample).toLowerCase();
        Container container = new Container();
        container.setImage(IMAGE);
        container.setName(containerName);
        List<String> command = arguments.asCommand(sample, SECRETS_PATH, serviceAccountKeySecretName);
        container.setCommand(command);
        container.setVolumeMounts(List.of(new VolumeMountBuilder().withMountPath(SAMPLES_PATH).withName(configMapName).build(),
                new VolumeMountBuilder().withMountPath(SECRETS_PATH).withName(serviceAccountKeySecretName).build()));
        return container;
    }
}
