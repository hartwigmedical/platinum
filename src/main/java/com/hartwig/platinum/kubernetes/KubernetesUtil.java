package com.hartwig.platinum.kubernetes;

public final class KubernetesUtil {
    public static String toValidRFC1123Label(String... input) {
        String label = String.join("-", input).replaceAll("[_\\s]+", "-").toLowerCase();

        if (label.length() > 63) {
            throw new IllegalArgumentException("Label is too long: " + label);
        }

        String labelRegex = "[a-z0-9]([-a-z0-9]*[a-z0-9])?";
        if (!label.matches(labelRegex)) {
            throw new IllegalArgumentException(String.format("Invalid label: \"%s\", should follow regex: \"%s\"", label, labelRegex));
        }

        return label;
    }
}
