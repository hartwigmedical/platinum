package com.hartwig.platinum.kubernetes.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.hartwig.platinum.config.GcpConfiguration;
import com.hartwig.platinum.config.PlatinumConfiguration;
import com.hartwig.platinum.config.ServiceAccountConfiguration;

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

    @Test(expected = IllegalArgumentException.class)
    public void doesNotAllowOverrideOfApi() {
        createVictimWithOverrides("hmf_api_url", "whatever");
    }

    private List<String> asCommand(final PipelineArguments victim) {
        return victim.asCommand(SampleArgument.sampleJson("tumor-run", "run"));
    }

    private PipelineArguments createVictimWithOverrides(final String key, final String value) {
        return new PipelineArguments(Map.of(key, value),
                "output",
                "email",
                PlatinumConfiguration.builder()
                        .gcp(GcpConfiguration.builder().region("region").project("project").privateCluster(false).build())
                        .serviceAccount(ServiceAccountConfiguration.builder()
                                .kubernetesServiceAccount("ksa")
                                .gcpEmailAddress("email")
                                .build())
                        .build());
    }
}