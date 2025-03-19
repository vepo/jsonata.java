package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

public record FieldMap(String field) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var currNode = current;
        if (currNode.hasField(field)) {
            currNode = currNode.get(field);
        } else {
            currNode = Mapping.empty();
        }
        return currNode;
    }
}