package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

public class WildcardJSONFunction implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        if (!current.isEmpty() && current.isObject()) {
            return current.all();
        } else {
            return current;
        }
    }

}