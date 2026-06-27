package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Post-parse tail-call wrapper: last expression in a function body that is a bare call.
 */
public record TailCallWrapper(Mapping inner) implements TailCallMapping {

    @Override
    public Data map(Data original, Data current) {
        return inner.map(original, current);
    }

    @Override
    public Data mapWithFrame(Data original, Data current, BlockContext frame) {
        return inner.map(original, current);
    }
}
