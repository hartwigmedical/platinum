package com.hartwig.platinum;

import org.immutables.value.Value;

@Value.Immutable
public interface PlatinumResult {

    int numSuccess();

    int numFailure();

    static PlatinumResult of(final int successes, final int failures) {
        return ImmutablePlatinumResult.builder().numSuccess(successes).numFailure(failures).build();
    }
}
