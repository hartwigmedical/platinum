package com.hartwig.platinum;

import java.io.File;

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
        PlatinumConfiguration configuration = PlatinumConfiguration.from(new File(input));
        KubernetesCluster cluster = KubernetesCluster.findOrCreate(runName,
                OutputBucket.from(storage).findOrCreate(runName, configuration.outputConfiguration()));
        cluster.submit(configuration);
    }
}
