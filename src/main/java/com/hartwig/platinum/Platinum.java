package com.hartwig.platinum;

import com.google.cloud.storage.Storage;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.kubernetes.KubernetesCluster;
import com.hartwig.platinum.storage.OutputBucket;

public class Platinum {

    private final String runName;
    private final String input;
    private String dataDirectory;
    private String location;
    private final Storage storage;

    public Platinum(final String runName, final String input, final String dataDirectory, final String location, final Storage storage) {
        this.runName = runName;
        this.input = input;
        this.dataDirectory = dataDirectory;
        this.location = location;
        this.storage = storage;
    }

    public void run() {
        PlatinumConfiguration configuration = PlatinumConfiguration.from(runName, input, dataDirectory, location);
        KubernetesCluster cluster = KubernetesCluster.findOrCreate(runName,
                OutputBucket.from(storage).findOrCreate(runName, configuration.outputConfiguration()));
        cluster.submit(configuration);
    }
}
