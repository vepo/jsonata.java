package dev.vepo.jsonata.results;

import static java.util.Collections.emptyList;

import java.util.List;

import dev.vepo.jsonata.JSONataResult;

/**
 * {@link JSONataResult} for the JSONata empty sequence.
 * <p>
 * Layer: <strong>domain</strong>. Created via {@link JSONataResults#empty()}.
 * {@link #isEmpty()} is {@code true}; scalar accessors throw {@link IllegalStateException}.
 * {@link #multi()} returns immutable empty lists without throwing.
 */
class JSONataEmptyResult implements JSONataResult {
    private static RuntimeException emptyValueException() {
        return new IllegalStateException("Value is empty");
    }

    JSONataEmptyResult() {
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException always — empty results have no scalar value
     */
    @Override
    public String asText() {
        throw emptyValueException();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException always — empty results have no scalar value
     */
    @Override
    public int asInt() {
        throw emptyValueException();
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public boolean isInt() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException always — empty results have no scalar value
     */
    @Override
    public double asDouble() {
        throw emptyValueException();
    }

    @Override
    public boolean isDouble() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException always — empty results have no scalar value
     */
    @Override
    public boolean asBoolean() {
        throw emptyValueException();
    }

    @Override
    public Multi multi() {
        return new Multi() {

            @Override
            public List<String> asText() {
                return emptyList();
            }

            @Override
            public List<Integer> asInt() {
                return emptyList();
            }

            @Override
            public List<Boolean> asBoolean() {
                return emptyList();
            }

            @Override
            public List<Double> asDouble() {
                return emptyList();
            }
        };
    }
}
