package com.hartwig.platinum.iam;

import org.immutables.value.Value;

@Value.Immutable
public interface JsonKey {

    @Value.Default
    default boolean secretExists() {
        return false;
    }

    @Value.Default
    default String secretName() {
        return "service-account-key";
    }

    String jsonBase64();

    static JsonKey existing(final String secret) {
        return ImmutableJsonKey.builder().secretName(secret).secretExists(true).jsonBase64("").build();
    }

    static JsonKey of(final String json) {
        return ImmutableJsonKey.builder().jsonBase64(json).build();
    }
}