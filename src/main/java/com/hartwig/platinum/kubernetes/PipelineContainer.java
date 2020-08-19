package com.hartwig.platinum.kubernetes;

import static java.lang.String.format;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;

public class PipelineContainer {

    private final static String SAMPLES_PATH = "/samples";
    private final static String SECRETS_PATH = "/secrets";
    private static final String IMAGE = "hartwigmedicalfoundation/pipeline5:5.13.1743";
    private final String sample;
    private final String runName;
    private final Map<String, String> argumentPairs;
    private final String outputBucket;
    private final String project;
    private final String serviceAccountEmail;
    private final String region;

    public PipelineContainer(final String sample, final String runName, final Map<String, String> argumentPairs, final String outputBucket,
            final String project, final String serviceAccountEmail, final String region) {
        this.sample = sample;
        this.runName = runName;
        this.argumentPairs = argumentPairs;
        this.outputBucket = outputBucket;
        this.project = project;
        this.serviceAccountEmail = serviceAccountEmail;
        this.region = region;
    }

    Container create(final String configMapName, final String serviceAccountKeySecretName) {
        String containerName = format("%s-%s", runName, sample).toLowerCase();
        Container container = new Container();
        container.setImage(IMAGE);
        container.setName(containerName);
        List<String> arguments = ImmutableList.<String>builder().add("/pipeline5.sh")
                .addAll(argumentPairs.entrySet().stream().flatMap(e -> Stream.of(e.getKey(), e.getValue())).collect(Collectors.toList()))
                .add("-profile",
                        "public",
                        "-set_id",
                        sample,
                        "-sample_json",
                        format("%s/%s", SAMPLES_PATH, sample),
                        "-output_bucket",
                        outputBucket,
                        "-private_key_path",
                        format("%s/%s", SECRETS_PATH, serviceAccountKeySecretName),
                        "-project",
                        project,
                        "-region",
                        region,
                        "-service_account_email",
                        serviceAccountEmail,
                        "-run_id",
                        runName)
                .build();
        container.setCommand(arguments);
        container.setVolumeMounts(List.of(new VolumeMountBuilder().withMountPath(SAMPLES_PATH).withName(configMapName).build(),
                new VolumeMountBuilder().withMountPath(SECRETS_PATH).withName(serviceAccountKeySecretName).build()));
        return container;
    }
}
