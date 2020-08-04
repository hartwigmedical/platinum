package com.hartwig.platinum.storage;

public class BucketName {

    static String of(final String runName) {
        return "platinum-output-" + runName;
    }
}
