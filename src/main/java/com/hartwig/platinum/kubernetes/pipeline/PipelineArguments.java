package com.hartwig.platinum.kubernetes.pipeline;

import com.google.common.collect.ImmutableMap;
import com.hartwig.platinum.config.GcpConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.hartwig.platinum.config.OverrideableArguments.of;

public class PipelineArguments {

    private final Map<String, String> overrides;
    private final String outputBucket;
    private final String serviceAccountEmail;
    private final PlatinumConfiguration platinumConfiguration;

    public PipelineArguments(final Map<String, String> overrides, final String outputBucket, final String serviceAccountEmail,
            final PlatinumConfiguration platinumConfiguration) {
        this.overrides = overrides;
        this.outputBucket = outputBucket;
        this.serviceAccountEmail = serviceAccountEmail;
        this.platinumConfiguration = platinumConfiguration;
    }

    public List<String> asCommand(final SampleArgument sampleArgument) {
        return of(Map.of("-profile", "public", "-output_cram", "false")).override(of(addDashesIfNeeded()))
                .override(of(sampleArgument.arguments()))
                .override(of(fixed()))
                .asCommand("/pipeline5.sh");
    }

    private Map<String, String> addDashesIfNeeded() {
        return overrides.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey().startsWith("-") ? e.getKey() : "-" + e.getKey(), Map.Entry::getValue));
    }

    private Map<String, String> fixed() {
        GcpConfiguration gcpConfiguration = platinumConfiguration.gcp();
        ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder()
                .put("-output_bucket", outputBucket)
                .put("-project", gcpConfiguration.projectOrThrow())
                .put("-region", gcpConfiguration.regionOrThrow())
                .put("-network", gcpConfiguration.networkUrl())
                .put("-subnet", gcpConfiguration.subnetUrl())
                .put("-service_account_email", serviceAccountEmail);
        if (!gcpConfiguration.networkTags().isEmpty()) {
            builder.put("-network_tags", String.join(",", gcpConfiguration.networkTags()));
        }
        if (platinumConfiguration.hmfConfiguration().isPresent() && platinumConfiguration.hmfConfiguration().get().isRerun()) {
            builder.put("-profile", "production");
            builder.put("-context", "RESEARCH");
        } else {
            builder.put("-context", "PLATINUM");
        }
        if (platinumConfiguration.cmek().isPresent()) {
            builder.put("-cmek", platinumConfiguration.cmek().get());
        }
        return builder.build();
    }
}
