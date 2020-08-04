package com.hartwig.platinum;

import java.util.List;

import com.google.api.services.iam.v1.Iam;
import com.google.cloud.storage.Storage;
import com.hartwig.platinum.config.RunConfiguration;
import com.hartwig.platinum.iam.PipelineServiceAccount;
import com.hartwig.platinum.jobs.PipelineResult;
import com.hartwig.platinum.kubernetes.KubernetesCluster;
import com.hartwig.platinum.storage.OutputBucket;

public class Platinum {

    private final String runName;
    private final String input;
    private final Storage storage;
    private final Iam iam;
    private final String project;

    public Platinum(final String runName, final String input, final Storage storage, final Iam iam, final String project) {
        this.runName = runName;
        this.input = input;
        this.storage = storage;
        this.iam = iam;
        this.project = project;
    }

    public PlatinumResult run() {
        RunConfiguration configuration = RunConfiguration.from(runName, input);
        PipelineServiceAccount serviceAccount = new PipelineServiceAccount(iam);
        String serviceAccountName = serviceAccount.findOrCreate(project, runName);
        KubernetesCluster cluster = KubernetesCluster.findOrCreate(runName,
                OutputBucket.from(storage).findOrCreate(runName, configuration.outputConfiguration()));
        List<PipelineResult> results = cluster.submit(configuration).get();
        serviceAccount.delete();
        return PlatinumResult.of(results);
    }
}
