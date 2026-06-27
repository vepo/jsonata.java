package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Explicit composition of two mappings ({@link Mapping#andThen}).
 */
public record ChainedMapping(Mapping first, Mapping second) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        return second.map(original, first.map(original, current));
    }

    static ObjectMapper terminalObjectMapper(Mapping mapping) {
        var current = mapping;
        while (current instanceof ChainedMapping chain) {
            current = chain.second();
        }
        return current instanceof ObjectMapper objectMapper ? objectMapper : null;
    }
}
