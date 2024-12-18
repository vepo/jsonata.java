package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.data.Data.empty;

import java.util.ArrayList;

import dev.vepo.jsonata.functions.data.GroupedData;
import dev.vepo.jsonata.functions.data.Data;

public record FieldPredicateJSONFunction(String fieldName, String content) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        if (!current.isArray()) {
            return current;
        }
        if (current.hasField(fieldName)) {
            var matched = new ArrayList<Data>();
            for (int i = 0; i < current.length(); ++i) {
                var inner = current.at(i);
                var innerContent = inner.get(fieldName).toJson();
                if (innerContent.asText().equals(content)) {
                    matched.add(inner);
                }
            }
            return new GroupedData(matched);
        } else {
            return empty();
        }
    }

}