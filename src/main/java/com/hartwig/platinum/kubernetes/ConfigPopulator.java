package com.hartwig.platinum.kubernetes;

import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Map.Entry;

import com.hartwig.platinum.config.PlatinumConfiguration;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class ConfigPopulator {
    private String keyJson;
    private final PlatinumConfiguration configuration;

    public ConfigPopulator(final String keyJson, final PlatinumConfiguration configuration) {
        this.keyJson = keyJson;
        this.configuration = configuration;
    }

    public void populate() {
        KubernetesClient client = new DefaultKubernetesClient();
        client.configMaps()
                .createNew()
                .addToData(configuration.samples().entrySet().stream().collect(toMap(Entry::getKey, e -> e.getValue().toString())))
                .withNewMetadata()
                .withName("samples")
                .endMetadata()
                .done();
        try {
            String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(new File(keyJson).toPath()));
            client.secrets().createNew().withNewMetadata().withName("compute-key").endMetadata().addToData("compute-key", base64).done();
        } catch (IOException e) {
            throw new RuntimeException("Could not populate key", e);
        }
    }
}
