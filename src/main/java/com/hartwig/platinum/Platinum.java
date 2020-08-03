package com.hartwig.platinum;

import com.hartwig.platinum.config.RunConfiguration;
import com.hartwig.platinum.kubernetes.KubernetesCluster;
import com.hartwig.platinum.storage.OutputBucket;

public class Platinum {

    private final String runName;
    private final String input;

    public Platinum(final String runName, final String input) {
        this.runName = runName;
        this.input = input;
    }

    public PlatinumResult run() {
        RunConfiguration configuration = RunConfiguration.from(runName, input);
        OutputBucket outputBucket = OutputBucket.findOrCreate(runName);
        KubernetesCluster cluster = KubernetesCluster.findOrCreate(runName, outputBucket);
        return PlatinumResult.of(cluster.submit(configuration).get());
    }
}
