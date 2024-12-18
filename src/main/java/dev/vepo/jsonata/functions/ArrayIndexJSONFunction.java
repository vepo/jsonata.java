package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

public record ArrayIndexJSONFunction(int index) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        if (!current.isArray()) {
            return current;
        }
        if (index >= 0 && index < current.length()) {
            return current.at(index);
        } else if (index < 0 && -index < current.length()) {
            return current.at(current.length() + index);
        } else {
            return JSONataFunction.empty();
        }
    }

}