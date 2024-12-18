package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;

public record FieldPathJSONFunction(List<String> fields) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        var currNode = current;
        for (var field : fields) {
            if (currNode.isEmpty()) {
                break;
            } else if (currNode.hasField(field)) {
                currNode = currNode.get(field);
            } else {
                currNode = JSONataFunction.empty();
            }
        }
        return currNode;
    }
}