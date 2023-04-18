package com.hartwig.platinum.kubernetes;

import static java.lang.String.format;

import java.util.List;
import java.util.Map;

import com.hartwig.platinum.config.PlatinumConfiguration;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;

public class PipelineContainer implements KubernetesComponent<Container> {
    private final static String SAMPLES_PATH = "/samples";
    private final static String SECRETS_PATH = "/secrets";
    private final SampleArgument sample;
    private final PipelineArguments arguments;
    private final String serviceAccountKeySecretName;
    private final String configMapName;
    private final String imageName;
    private final PlatinumConfiguration configuration;

    public PipelineContainer(final SampleArgument sample, final PipelineArguments arguments,
            final String serviceAccountKeySecretName, final String configMapName, final String imageName,
            final PlatinumConfiguration configuration) {
        this.sample = sample;
        this.arguments = arguments;
        this.serviceAccountKeySecretName = serviceAccountKeySecretName;
        this.configMapName = configMapName;
        this.imageName = imageName;
        this.configuration = configuration;
    }

    @Override
    public Container asKubernetes() {
        String containerName = sample.id();
        Container container = new Container();
        container.setImage(imageName);
        container.setName(containerName);
        List<String> command = arguments.asCommand(sample, SECRETS_PATH, serviceAccountKeySecretName);
        container.setCommand(command);
        container.setVolumeMounts(List.of(new VolumeMountBuilder().withMountPath(SAMPLES_PATH).withName(configMapName).build(),
                new VolumeMountBuilder().withMountPath(SECRETS_PATH).withName(serviceAccountKeySecretName).build()));
        if (configuration.samples().isEmpty() && configuration.sampleBucket().isEmpty()) {
            final ResourceRequirements resourceRequirements = new ResourceRequirements();
            final Map<String, Quantity> resources = Map.of("cpu", new Quantity("100m"), "memory", new Quantity("512Mi"));
            resourceRequirements.setLimits(resources);
            resourceRequirements.setRequests(resources);
            container.setResources(resourceRequirements);
        }
        return container;
    }
}
