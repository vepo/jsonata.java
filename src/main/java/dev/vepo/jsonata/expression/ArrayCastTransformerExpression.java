package dev.vepo.jsonata.expression;

import static java.util.Collections.singletonList;

import dev.vepo.jsonata.expression.transformers.Value;
import dev.vepo.jsonata.expression.transformers.Value.GroupedValue;

public class ArrayCastTransformerExpression implements Expression {

    @Override
    public Value map(Value original, Value current) {
        if (current.isObject()) {
            return new GroupedValue(singletonList(current));
        } else {
            return current;
        }
    }
}