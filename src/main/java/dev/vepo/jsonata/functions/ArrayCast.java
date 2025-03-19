package dev.vepo.jsonata.functions;

import static java.util.Collections.singletonList;

import dev.vepo.jsonata.functions.data.GroupedData;
import dev.vepo.jsonata.functions.data.Data;

public class ArrayCast implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        if (current.isObject()) {
            return new GroupedData(singletonList(current));
        } else {
            return current;
        }
    }
}