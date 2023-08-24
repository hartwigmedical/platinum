package com.hartwig.platinum.kubernetes.pipeline;

import com.hartwig.platinum.config.GcpConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
        return victim.asCommand(SampleArgument.sampleJson("tumor-run", "run"));
    }

    private PipelineArguments createVictimWithOverrides(final String s, final String value) {
        return new PipelineArguments(Map.of(s, value),
                "output",
                "email",
                PlatinumConfiguration.builder()
                        .gcp(GcpConfiguration.builder().region("region").project("project").privateCluster(false).build())
                        .build());
    }
}