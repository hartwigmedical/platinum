package com.hartwig.platinum.config;

import org.junit.Test;

public class ValidationTest {

    @Test(expected = IllegalArgumentException.class)
    public void failsValidationWhenSampleNamePlusRunNameExceeds49() {
        Validation.apply("this-is-a-long-run-name", configWithSampleName("this-is-a-long-sample-name"));
    }

    @Test
    public void passesValidation() {
        Validation.apply("run", configWithSampleName("sample"));
    }

    private PlatinumConfiguration configWithSampleName(String sampleName) {
        return PlatinumConfiguration.builder()
                .addSamples(SampleConfiguration.builder()
                        .name(sampleName)
                        .addTumors(RawDataConfiguration.builder().name("tumor").build())
                        .normal(RawDataConfiguration.builder().name("normal").build())
                        .build())
                .serviceAccount(ServiceAccountConfiguration.builder().kubernetesServiceAccount("ksa").gcpEmailAddress("email").build())
                .build();
    }
}