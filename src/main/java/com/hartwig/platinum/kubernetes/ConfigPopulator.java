package com.hartwig.platinum.kubernetes;

import static java.util.stream.Collectors.toMap;

import java.util.Map.Entry;

import com.hartwig.platinum.config.PlatinumConfiguration;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class ConfigPopulator {
    private final String namespace;
    private final PlatinumConfiguration configuration;

    public ConfigPopulator(final String namespace, final PlatinumConfiguration configuration) {
        this.namespace = namespace;
        this.configuration = configuration;
    }

    public Volume populateJson() {
        KubernetesClient client = new DefaultKubernetesClient();
        ConfigMap samples = client.configMaps()
                .createNew()
                .addToData(configuration.samples().entrySet().stream().collect(toMap(Entry::getKey, e -> e.getValue().toString())))
                .withNewMetadata()
                .withName("samples")
                .withNamespace(namespace)
                .endMetadata()
                .done();
        return null;
//        Volume volume = new Volume();
//        ConfigMapVolumeSource source = new ConfigMapVolumeSource();
//        source.setName("samples");
//        volume.setConfigMap(

    }
}
