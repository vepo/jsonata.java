package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

public record FieldMapJSONataFunction(String field) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        var currNode = current;
        if (currNode.hasField(field)) {
            currNode = currNode.get(field);
        } else {
            currNode = JSONataFunction.empty();
        }
        return currNode;
    }
}