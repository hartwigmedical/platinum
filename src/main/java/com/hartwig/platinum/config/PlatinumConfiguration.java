package com.hartwig.platinum.config;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(jdkOnly = true)
@JsonDeserialize(as = ImmutablePlatinumConfiguration.class)
public interface PlatinumConfiguration {

    @Value.Default
    default String image() {
        return "eu.gcr.io/hmf-images/pipeline5:public-v5.29";
    }

    @Value.Default
    default GcpConfiguration gcp() {
        return GcpConfiguration.builder().build();
    }

    @Value.Default
    default boolean namespaced() {
        return false;
    }

    @Value.Default
    default boolean inCluster() {
        return false;
    }

    Optional<BatchConfiguration> batch();

    Optional<String> outputBucket();

    Optional<String> cmek();

    Optional<ServiceAccountConfiguration> serviceAccount();

    Optional<String> cluster();

    Optional<HmfConfiguration> hmfConfiguration();

    Optional<String> keystorePassword();

    default PlatinumConfiguration withGcp(final GcpConfiguration gcp) {
        return builder().from(this).gcp(gcp).build();
    }

    Map<String, String> argumentOverrides();

    List<SampleConfiguration> samples();

    List<String> sampleIds();

    Optional<String> sampleBucket();

    @Value.Default
    default boolean createRun() {
        return false;
    }

    @Value.Default
    default boolean retryFailed() {
        return false;
    }

    static ImmutablePlatinumConfiguration.Builder builder() {
        return ImmutablePlatinumConfiguration.builder();
    }

    static PlatinumConfiguration from(final String inputFile) {
        ObjectMapper objectMapper = inputFile.endsWith("yaml") ? new ObjectMapper(new YAMLFactory()) : new ObjectMapper();
        objectMapper.findAndRegisterModules();
        try {
            PlatinumConfiguration platinumConfiguration = objectMapper.readValue(new File(inputFile), new TypeReference<>() {
            });
            if (!platinumConfiguration.samples().isEmpty() && !platinumConfiguration.sampleIds().isEmpty()) {
                throw new IllegalArgumentException("Cannot specify both a list of sample jsons and a list of biopsies. "
                        + "Split this configuration into two platinum runs.");
            }
            return platinumConfiguration;
        } catch (IOException ioe) {
            throw new RuntimeException("Could not parse input", ioe);
        }
    }
}

