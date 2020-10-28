package com.hartwig.platinum;

import static java.lang.String.format;

import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;

@Value.Immutable
public interface GcpConfiguration {

    String DEFAULT_NETWORK_NAME = "default";

    String project();

    String region();

    boolean privateCluster();

    Optional<String> secondaryRangeNamePods();

    Optional<String> secondaryRangeNameServices();

    @Value.Default
    default String network() {
        return DEFAULT_NETWORK_NAME;
    }

    @Value.Default
    default String subnet() {
        return DEFAULT_NETWORK_NAME;
    }

    default String networkUrl() {
        return isUrl(network()) ? network() : format("projects/%s/global/networks/%s", project(), network());
    }

    default String subnetUrl() {
        return isUrl(subnet()) ? subnet() : format("projects/%s/regions/%s/subnetworks/%s", project(), region(), subnet());
    }

    private boolean isUrl(final String argument) {
        return argument.startsWith("projects");
    }

    List<String> networkTags();

    static ImmutableGcpConfiguration.Builder builder() {
        return ImmutableGcpConfiguration.builder();
    }
}