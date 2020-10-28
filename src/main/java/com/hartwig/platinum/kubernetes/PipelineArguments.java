package com.hartwig.platinum.kubernetes;

import static java.lang.String.format;

import static com.hartwig.platinum.config.OverrideableArguments.of;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.hartwig.platinum.GcpConfiguration;

public class PipelineArguments {

    private final Map<String, String> overrides;
    private final String outputBucket;
    private final String serviceAccountEmail;
    private final String runName;
    private final GcpConfiguration gcpConfiguration;

    public PipelineArguments(final Map<String, String> overrides, final String outputBucket, final String serviceAccountEmail,
            final String runName, final GcpConfiguration gcpConfiguration) {
        this.overrides = overrides;
        this.outputBucket = outputBucket;
        this.serviceAccountEmail = serviceAccountEmail;
        this.runName = runName;
        this.gcpConfiguration = gcpConfiguration;
    }

    public List<String> asCommand(final SampleArgument sampleArgument, final String secretsPath, final String serviceAccountKeySecretName) {
        return of(Map.of("-profile", "public", "-output_cram", "false")).override(of(addDashesIfNeeded()))
                .override(of(fixed(secretsPath, serviceAccountKeySecretName)))
                .override(of(Map.of(sampleArgument.argument(), sampleArgument.value())))
                .asCommand("/pipeline5.sh");
    }

    private Map<String, String> addDashesIfNeeded() {
        return overrides.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey().startsWith("-") ? e.getKey() : "-" + e.getKey(), Map.Entry::getValue));
    }

    private Map<String, String> fixed(final String secretsPath, final String serviceAccountKeySecretName) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder().put("-output_bucket", outputBucket)
                .put("-private_key_path", format("%s/%s", secretsPath, serviceAccountKeySecretName))
                .put("-project", gcpConfiguration.project())
                .put("-region", gcpConfiguration.region())
                .put("-network", gcpConfiguration.networkUrl())
                .put("-subnet", gcpConfiguration.subnetUrl())
                .put("-service_account_email", serviceAccountEmail)
                .put("-run_id", runName);
        if (!gcpConfiguration.networkTags().isEmpty()) {
            builder.put("-network_tags", String.join(",", gcpConfiguration.networkTags()));
        }
        return builder.build();
    }
}
