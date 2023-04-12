package com.hartwig.platinum.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.hartwig.pdl.PipelineInput;
import com.hartwig.pdl.SampleInput;
import com.hartwig.platinum.config.GcpConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.p5sample.ImmutableSample;
import com.hartwig.platinum.p5sample.ImmutableTumorNormalPair;

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
        return victim.asCommand(SampleArgument.sampleJson(PipelineInput.builder()
                .setName("sample")
                .tumor(SampleInput.builder().name("tumor").build())
                .reference(SampleInput.builder().name("normal").build())
                .build(), "run"), "/secrets", "key.json");
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