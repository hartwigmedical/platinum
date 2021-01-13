package com.hartwig.platinum.config;

import static java.lang.String.format;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableGcpConfiguration.class)
@Value.Style(jdkOnly = true)
public interface GcpConfiguration {

    String DEFAULT_NETWORK_NAME = "default";

    Optional<String> project();

    Optional<String> region();

    default String projectOrThrow() {
        return project().orElseThrow(missing("project"));
    }

    default String regionOrThrow() {
        return region().orElseThrow(missing("region"));
    }

    default GcpConfiguration withRegion(final String region) {
        return GcpConfiguration.builder().from(this).region(region).build();
    }

    default GcpConfiguration withProject(final String project) {
        return GcpConfiguration.builder().from(this).project(project).build();
    }

    @Value.Default
    default boolean privateCluster() {
        return false;
    }

    @Value.Default
    default int maxNodes() {
        return 10;
    }

    List<String> zones();

    Optional<String> secondaryRangeNamePods();

    Optional<String> secondaryRangeNameServices();

    @Value.Default
    default String masterIpv4CidrBlock() {
        return "172.16.0.32/28";
    }

    @Value.Default
    default String network() {
        return DEFAULT_NETWORK_NAME;
    }

    @Value.Default
    default String subnet() {
        return DEFAULT_NETWORK_NAME;
    }

    default String networkUrl() {
        return isUrl(network()) ? network() : format("projects/%s/global/networks/%s", projectOrThrow(), network());
    }

    default String subnetUrl() {
        return isUrl(subnet()) ? subnet() : format("projects/%s/regions/%s/subnetworks/%s", projectOrThrow(), regionOrThrow(), subnet());
    }

    private boolean isUrl(final String argument) {
        return argument.startsWith("projects");
    }

    List<String> networkTags();

    static ImmutableGcpConfiguration.Builder builder() {
        return ImmutableGcpConfiguration.builder();
    }

    private Supplier<IllegalStateException> missing(final String field) {
        return () -> new IllegalStateException(String.format("[%s] was not configured. You can configure [%s] in either the CLI or gcp json",
                field,
                field));
    }

}