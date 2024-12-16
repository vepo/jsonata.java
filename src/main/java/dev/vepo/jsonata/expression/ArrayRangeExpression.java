package dev.vepo.jsonata.expression;

import static dev.vepo.jsonata.expression.transformers.Value.empty;
import static java.lang.Math.min;
import static java.util.stream.IntStream.range;

import dev.vepo.jsonata.expression.transformers.Value;
import dev.vepo.jsonata.expression.transformers.Value.GroupedValue;

public record ArrayRangeExpression(int start, int end) implements Expression {

    @Override
    public Value map(Value original, Value current) {
        if (!current.isArray()) {
            return current;
        }
        if (start < current.lenght()) {
            return new GroupedValue(range(start, min(end + 1, current.lenght())).mapToObj(current::at) 
                                                                                .toList());
        } else {
            return empty();
        }
    }

}