package com.hartwig.platinum;

import com.google.cloud.storage.Storage;
import com.hartwig.platinum.config.RunConfiguration;
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

    public PlatinumResult run() {
        RunConfiguration configuration = RunConfiguration.from(runName, input);
        KubernetesCluster cluster = KubernetesCluster.findOrCreate(runName,
                OutputBucket.from(storage).findOrCreate(runName, configuration.outputConfiguration()));
        return PlatinumResult.of(cluster.submit(configuration).get());
    }
}
