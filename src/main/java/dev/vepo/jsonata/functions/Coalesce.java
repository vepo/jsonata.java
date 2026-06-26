package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

public record Coalesce(Mapping left, Mapping right) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var leftValue = left.map(original, current);
        if (leftValue == null || leftValue.isEmpty()) {
            return right.map(original, current);
        }
        return leftValue;
    }
}
