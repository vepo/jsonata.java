package dev.vepo.jsonata.expression;

import static dev.vepo.jsonata.expression.transformers.JsonFactory.arrayNode;
import static dev.vepo.jsonata.expression.transformers.JsonFactory.json2Value;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import dev.vepo.jsonata.expression.transformers.Value;
import dev.vepo.jsonata.expression.transformers.Value.GroupedValue;

public record ArrayConstructorExpression(List<Function<Value, Value>> arrayBuilder) implements Expression {

    @Override
    public Value map(Value original, Value current) {
        if (current.isArray() && arrayBuilder.size() == 1) {
            var elements = new ArrayList<Value>();
            for (int i=0; i < current.lenght();++i) {
                elements.add(arrayBuilder.get(0).apply(current.at(i)));
            }
            return json2Value(new GroupedValue(elements).toJson());
        } else {
            return json2Value(arrayNode(arrayBuilder.stream().map(fn -> fn.apply(current).toJson()).toList()));
        }
    }
}