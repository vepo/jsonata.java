package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Sequential composition of two mappings ({@link Mapping#andThen}).
 *
 * <p>Represents chained path steps where the output of {@code first} becomes the
 * {@code current} focus for {@code second}; {@code original} is forwarded unchanged.
 *
 * @param first  the mapping evaluated first
 * @param second the mapping evaluated with the first result as focus
 */
public record ChainedMapping(Mapping first, Mapping second) implements Mapping {

    /** {@inheritDoc} */
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
