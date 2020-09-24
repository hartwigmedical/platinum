package com.hartwig.platinum.kubernetes;

import static java.lang.String.format;

import static com.hartwig.platinum.config.OverrideableArguments.*;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.hartwig.platinum.config.OverrideableArguments;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;

public class PipelineContainer {
    private final static String SAMPLES_PATH = "/samples";
    private final static String SECRETS_PATH = "/secrets";
    private static final String IMAGE = "hartwigmedicalfoundation/pipeline5:platinum";
    private final String sample;
    private final String runName;
    private final PipelineArguments arguments;

    public PipelineContainer(final String sample, final String runName, final PipelineArguments arguments) {
        this.sample = sample;
        this.runName = runName;
        this.arguments = arguments;
    }

    Container create(final String configMapName, final String serviceAccountKeySecretName) {
        String containerName = format("%s-%s", runName, sample).toLowerCase();
        Container container = new Container();
        container.setImage(IMAGE);
        container.setName(containerName);
        List<String> command = arguments.asCommand(SAMPLES_PATH, SECRETS_PATH, serviceAccountKeySecretName);
        container.setCommand(command);
        container.setVolumeMounts(List.of(new VolumeMountBuilder().withMountPath(SAMPLES_PATH).withName(configMapName).build(),
                new VolumeMountBuilder().withMountPath(SECRETS_PATH).withName(serviceAccountKeySecretName).build()));
        return container;
    }
}
