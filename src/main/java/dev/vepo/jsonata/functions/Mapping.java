package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.EmptyData;

@FunctionalInterface
public interface Mapping {
    Data map(Data original, Data current);

    public static Data empty() {
        return new EmptyData();
    }

    default Mapping andThen(Mapping after) {
        return (original, current) -> after.map(original, map(original, current));
    }
}
