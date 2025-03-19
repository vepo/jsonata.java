package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

public record ArrayIndex(int index) implements Mapping {

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
            return Mapping.empty();
        }
    }

}