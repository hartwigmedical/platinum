package com.hartwig.platinum.iam;

import org.immutables.value.Value;

@Value.Immutable
public interface JsonKey {

    @Value.Parameter
    String id();

    @Value.Parameter
    String jsonBase64();

    static JsonKey of(final String id, final String json) {
        return ImmutableJsonKey.of(id, json);
    }
}