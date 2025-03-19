package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

public record MappingJoin(Mapping first, Mapping second) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        return second.map(original, first.map(original, current));
    }

}
