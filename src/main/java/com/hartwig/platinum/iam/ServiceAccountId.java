package com.hartwig.platinum.iam;

public class ServiceAccountId {

    static String from(final String project, final String runName) {
        return "platinum-" + runName;
    }
}
