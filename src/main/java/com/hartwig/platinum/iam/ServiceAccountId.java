package com.hartwig.platinum.iam;

public class ServiceAccountId {

    static String from(final String project, final String email) {
        return String.format("projects/%s/serviceAccounts/%s", project, email);
    }
}
