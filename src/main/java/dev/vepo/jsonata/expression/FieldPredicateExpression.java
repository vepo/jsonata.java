package dev.vepo.jsonata.expression;

import static dev.vepo.jsonata.expression.transformers.Value.empty;

import java.util.ArrayList;

import dev.vepo.jsonata.expression.transformers.Value;
import dev.vepo.jsonata.expression.transformers.Value.GroupedValue;

public record FieldPredicateExpression(String fieldName, String content) implements Expression {

    @Override
    public Value map(Value original, Value current) {
        if (!current.isArray()) {
            return current;
        }
        if (current.hasField(fieldName)) {
            var matched = new ArrayList<Value>();
            for (int i = 0; i < current.lenght(); ++i) {
                var inner = current.at(i);
                var innerContent = inner.get(fieldName).toJson();
                if (innerContent.asText().equals(content)) {
                    matched.add(inner);
                }
            }
            return new GroupedValue(matched);
        } else {
            return empty();
        }
    }

}