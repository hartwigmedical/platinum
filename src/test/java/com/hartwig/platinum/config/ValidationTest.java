package com.hartwig.platinum.config;

import org.junit.Test;

public class ValidationTest {

    @Test(expected = IllegalArgumentException.class)
    public void failsValidationWhenSampleNameAndTumorNameEqual() {
        Validation.apply("tumor",
                PlatinumConfiguration.builder()
                        .addSamples(SampleConfiguration.builder()
                                .name("tumor")
                                .addTumors(RawDataConfiguration.builder().name("tumor").build())
                                .normal(RawDataConfiguration.builder().name("normal").build())
                                .build())
                        .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsValidationWhenSampleNameAndNormalNameEqual() {
        Validation.apply("tumor",
                PlatinumConfiguration.builder()
                        .addSamples(SampleConfiguration.builder()
                                .name("normal")
                                .addTumors(RawDataConfiguration.builder().name("tumor").build())
                                .normal(RawDataConfiguration.builder().name("normal").build())
                                .build())
                        .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsValidationWhenSampleNamePlusRunNameExceeds49() {
        Validation.apply("this-is-a-long-run-name",
                PlatinumConfiguration.builder()
                        .addSamples(SampleConfiguration.builder()
                                .name("this-is-a-long-sample-name")
                                .addTumors(RawDataConfiguration.builder().name("tumor").build())
                                .normal(RawDataConfiguration.builder().name("normal").build())
                                .build())
                        .build());
    }

    @Test
    public void passesValidation() {
        Validation.apply("run",
                PlatinumConfiguration.builder()
                        .addSamples(SampleConfiguration.builder()
                                .name("sample")
                                .addTumors(RawDataConfiguration.builder().name("tumor").build())
                                .normal(RawDataConfiguration.builder().name("normal").build())
                                .build())
                        .build());
    }
}