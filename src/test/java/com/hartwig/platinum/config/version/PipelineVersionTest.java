package com.hartwig.platinum.config.version;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class PipelineVersionTest {
    @Test
    public void shouldPadVersionWithSingleNumber() {
        assertThat(new PipelineVersion().padVersionStringToSemanticVersion("4")).isEqualTo("4.0.0");
    }

    @Test
    public void shouldPadVersionWithTwoNumbers() {
        assertThat(new PipelineVersion().padVersionStringToSemanticVersion("4.0")).isEqualTo("4.0.0");
    }

    @Test
    public void shouldReturnFullVersionUnchanged() {
        assertThat(new PipelineVersion().padVersionStringToSemanticVersion("4.0.0")).isEqualTo("4.0.0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfCandidateHasTooManyTuples() {
        new PipelineVersion().padVersionStringToSemanticVersion("1.2.3.4");
    }

    @Test
    public void shouldExtractPipelineVersionFromDockerUrl() {
        assertThat(new PipelineVersion().extractVersionFromDockerImageName("eu.gcr.io/hmf-build/pipeline5:5.33.5")).isEqualTo("5.33.5");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfDockerUrlContainsNoVersionDelimiter() {
        new PipelineVersion().extractVersionFromDockerImageName("eu.gcr.io/hmf-build/pipeline5.5.33");
    }
}