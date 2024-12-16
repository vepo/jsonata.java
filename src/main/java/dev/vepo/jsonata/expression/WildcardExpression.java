package dev.vepo.jsonata.expression;

import dev.vepo.jsonata.expression.transformers.Value;

public class WildcardExpression implements Expression {

    @Override
    public Value map(Value original, Value current) {
        if (!current.isEmpty() && current.isObject()) {
            return current.all();
        } else {
            return current;
        }
    }

}