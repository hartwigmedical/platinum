package com.hartwig.platinum.config;

public class Validation {

    private static final int MAX_LENGTH = 49;

    public static void apply(final String runName, final PlatinumConfiguration configuration) {
        checkSamplePlusRunNameIsNotTooLong(runName, configuration);
    }

    private static void checkSamplePlusRunNameIsNotTooLong(final String runName, final PlatinumConfiguration configuration) {
        for (SampleConfiguration sample : configuration.samples()) {
            if (runName.length() + sample.name().length() >= MAX_LENGTH) {
                throw new IllegalArgumentException(String.format(
                        "Invalid input: Combination of run name [%s] and sample name [%s] should not exceed [%s] characters. Shorten one of them.",
                        runName,
                        sample.name(),
                        MAX_LENGTH));
            }
        }
    }
}
