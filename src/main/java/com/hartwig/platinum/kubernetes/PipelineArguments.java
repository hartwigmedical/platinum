package com.hartwig.platinum.kubernetes;

import static java.lang.String.format;

import static com.hartwig.platinum.config.OverrideableArguments.of;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

public class PipelineArguments {

    private final Map<String, String> overrides;
    private final String outputBucket;
    private final String project;
    private final String serviceAccountEmail;
    private final String region;
    private final String sample;
    private final String runName;

    public PipelineArguments(final Map<String, String> overrides, final String outputBucket, final String project,
            final String serviceAccountEmail, final String region, final String sample, final String runName) {
        this.overrides = overrides;
        this.outputBucket = outputBucket;
        this.project = project;
        this.serviceAccountEmail = serviceAccountEmail;
        this.region = region;
        this.sample = sample;
        this.runName = runName;
    }

    public List<String> asCommand(final String samplesPath, final String secretsPath, final String serviceAccountKeySecretName) {
        return of(Map.of("-profile", "public", "-output_cram", "false")).override(of(addDashesIfNeeded()))
                .override(of(fixed(samplesPath, secretsPath, serviceAccountKeySecretName)))
                .asCommand("/pipeline5.sh");
    }

    private Map<String, String> addDashesIfNeeded() {
        return overrides.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey().startsWith("-") ? e.getKey() : "-" + e.getKey(), Map.Entry::getValue));
    }

    private Map<String, String> fixed(final String samplesPath, final String secretsPath, final String serviceAccountKeySecretName) {
        return ImmutableMap.<String, String>builder().put("-set_id", sample)
                .put("-sample_json", format("%s/%s", samplesPath, sample))
                .put("-output_bucket", outputBucket)
                .put("-private_key_path", format("%s/%s", secretsPath, serviceAccountKeySecretName))
                .put("-project", project)
                .put("-region", region)
                .put("-service_account_email", serviceAccountEmail)
                .put("-run_id", runName)
                .build();
    }
}
