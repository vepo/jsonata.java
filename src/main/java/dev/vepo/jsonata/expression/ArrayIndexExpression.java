package dev.vepo.jsonata.expression;

import static dev.vepo.jsonata.expression.transformers.Value.empty;

import dev.vepo.jsonata.expression.transformers.Value;

public record ArrayIndexExpression(int index) implements Expression {

    @Override
    public Value map(Value original, Value current) {
        if (!current.isArray()) {
            return current;
        }
        if (index >= 0 && index < current.lenght()) {
            return current.at(index);
        } else if (index < 0 && -index < current.lenght()) {
            return current.at(current.lenght() + index);
        } else {
            return empty();
        }
    }

}