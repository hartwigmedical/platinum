package com.hartwig.platinum.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.immutables.value.Value;

@Value.Immutable
public interface OverrideableArguments {

    Map<String, String> arguments();

    default List<String> asCommand(final String commandName) {
        return Stream.concat(Stream.of(commandName), arguments().entrySet().stream().flatMap(e -> Stream.of(e.getKey(), e.getValue())))
                .collect(Collectors.toList());
    }

    default OverrideableArguments override(OverrideableArguments arguments) {
        Map<String, String> merged = new HashMap<>();
        merged.putAll(arguments());
        merged.putAll(arguments.arguments());
        return of(merged);
    }

    static OverrideableArguments of(Map<String, String> argumentPairs) {
        return ImmutableOverrideableArguments.builder().arguments(argumentPairs).build();
    }

}
