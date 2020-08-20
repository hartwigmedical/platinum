package com.hartwig.platinum;

public class Console {
    private static final String RESET = "\033[0m";
    private static final String BLACK_BOLD = "\033[1;30m";

    public static String bold(final String text) {
        return BLACK_BOLD + text + RESET;
    }
}
