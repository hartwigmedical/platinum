package com.hartwig.platinum.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.hartwig.platinum.config.GcpConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;

import org.junit.Test;

public class PipelineArgumentsTest {

    @Test
    public void addsNewOverrideIfNotInDefault() {
        PipelineArguments victim = createVictimWithOverrides("-arg", "value");
        assertThat(asCommand(victim)).containsOnlyOnce("-arg", "value");
    }

    @Test
    public void addsDashIfNotInArgument() {
        PipelineArguments victim = createVictimWithOverrides("arg", "value");
        assertThat(asCommand(victim)).containsOnlyOnce("-arg", "value");
    }

    @Test
    public void doesNotOverrideFixedValues() {
        PipelineArguments victim = createVictimWithOverrides("-region", "different");
        assertThat(asCommand(victim)).containsOnlyOnce("-region", "region");
    }

    @Test
    public void allowsOverridingOfDefaults() {
        PipelineArguments victim = createVictimWithOverrides("-output_cram", "true");
        assertThat(asCommand(victim)).containsOnlyOnce("-output_cram", "true");
    }

    private List<String> asCommand(final PipelineArguments victim) {
        return victim.asCommand(SampleArgument.sampleJson("sample"), "/secrets", "key.json");
    }

    private PipelineArguments createVictimWithOverrides(final String s, final String value) {
        return new PipelineArguments(Map.of(s, value),
                "output",
                "email",
                "run",
                PlatinumConfiguration.builder()
                        .gcp(GcpConfiguration.builder().region("region").project("project").privateCluster(false).build())
                        .build());
    }
}