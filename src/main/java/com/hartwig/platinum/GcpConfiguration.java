package com.hartwig.platinum;

import java.util.List;

import org.immutables.value.Value;

@Value.Immutable
public interface GcpConfiguration {

    String DEFAULT_NETWORK_NAME = "default";

    String project();

    String region();

    @Value.Default
    default String network() {
        return DEFAULT_NETWORK_NAME;
    }

    @Value.Default
    default String subnet() {
        return DEFAULT_NETWORK_NAME;
    }

    List<String> networkTags();

    static ImmutableGcpConfiguration.Builder builder() {
        return ImmutableGcpConfiguration.builder();
    }
}