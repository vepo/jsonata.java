package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * JSONata field navigation step ({@code .fieldName}).
 *
 * @param field the object field name to select from {@code current}
 */
public record FieldMap(String field) implements Mapping {

    /** {@inheritDoc} */
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
