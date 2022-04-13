package com.hartwig.platinum.kubernetes;

import static java.lang.String.format;

import static com.hartwig.platinum.config.OverrideableArguments.of;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.hartwig.platinum.config.GcpConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;

public class PipelineArguments {

    private final Map<String, String> overrides;
    private final String outputBucket;
    private final String serviceAccountEmail;
    private final String runName;
    private final PlatinumConfiguration platinumConfiguration;

    public PipelineArguments(final Map<String, String> overrides, final String outputBucket, final String serviceAccountEmail,
            final String runName, final PlatinumConfiguration platinumConfiguration) {
        this.overrides = overrides;
        this.outputBucket = outputBucket;
        this.serviceAccountEmail = serviceAccountEmail;
        this.runName = runName;
        this.platinumConfiguration = platinumConfiguration;
    }

    public List<String> asCommand(final SampleArgument sampleArgument, final String secretsPath, final String serviceAccountKeySecretName) {
        return of(Map.of("-profile", "public", "-output_cram", "false")).override(of(addDashesIfNeeded()))
                .override(of(fixed(secretsPath, serviceAccountKeySecretName)))
                .override(of(sampleArgument.arguments()))
                .asCommand("/pipeline5.sh");
    }

    private Map<String, String> addDashesIfNeeded() {
        return overrides.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey().startsWith("-") ? e.getKey() : "-" + e.getKey(), Map.Entry::getValue));
    }

    private Map<String, String> fixed(final String secretsPath, final String serviceAccountKeySecretName) {
        GcpConfiguration gcpConfiguration = platinumConfiguration.gcp();
        ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder().put("-output_bucket", outputBucket)
                .put("-private_key_path", format("%s/%s", secretsPath, serviceAccountKeySecretName))
                .put("-project", gcpConfiguration.projectOrThrow())
                .put("-region", gcpConfiguration.regionOrThrow())
                .put("-network", gcpConfiguration.networkUrl())
                .put("-subnet", gcpConfiguration.subnetUrl())
                .put("-service_account_email", serviceAccountEmail)
                .put("-context", "PLATINUM");
        if (!gcpConfiguration.networkTags().isEmpty()) {
            builder.put("-network_tags", String.join(",", gcpConfiguration.networkTags()));
        }
        if (platinumConfiguration.apiUrl().isPresent()) {
            builder.put("-profile", "production");
            builder.put("-context", "RESEARCH");
        }
        if (platinumConfiguration.cmek().isPresent()) {
            builder.put("-cmek", platinumConfiguration.cmek().get());
        }
        return builder.build();
    }
}
