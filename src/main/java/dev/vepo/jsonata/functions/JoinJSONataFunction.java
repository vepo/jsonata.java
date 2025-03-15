package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

public record JoinJSONataFunction(JSONataFunction first, JSONataFunction second) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        return second.map(original, first.map(original, current));
    }

}
