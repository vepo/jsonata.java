package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

public record ParentReference() implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        return PathBindings.parent(1).orElseGet(Mapping::empty);
    }
}
