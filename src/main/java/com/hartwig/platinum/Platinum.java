package com.hartwig.platinum;

import java.io.File;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.cloud.storage.Storage;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.kubernetes.KubernetesCluster;
import com.hartwig.platinum.storage.OutputBucket;

public class Platinum {

    private final String runName;
    private final String input;
    private final Storage storage;

    public Platinum(final String runName, final String input, final Storage storage) {
        this.runName = runName;
        this.input = input;
        this.storage = storage;
    }

    public void run() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        try {
            PlatinumConfiguration configuration = PlatinumConfiguration.builder()
                    .from(objectMapper.readValue(new File(input), new TypeReference<PlatinumConfiguration>() {})).runName(runName).build();
            KubernetesCluster cluster = KubernetesCluster.findOrCreate(runName,
                    OutputBucket.from(storage).findOrCreate(runName, configuration.outputConfiguration()));
            cluster.submit(configuration);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }
}
