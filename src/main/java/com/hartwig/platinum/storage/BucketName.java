package com.hartwig.platinum.storage;

public class BucketName {

    static String from(final String runName) {
        return "platinum-output-" + runName;
    }
}
