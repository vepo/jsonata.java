package dev.vepo.jsonata.expression;

import static dev.vepo.jsonata.expression.transformers.Value.empty;

import java.util.List;

import dev.vepo.jsonata.expression.transformers.Value;

public record FieldPathExpression(List<String> fields) implements Expression {

    @Override
    public Value map(Value original, Value current) {
        var currNode = current;
        for (var field : fields) {
            if (currNode.isEmpty()) {
                break;
            } else if (currNode.hasField(field)) {
                currNode = currNode.get(field);
            } else {
                currNode = empty();
            }
        }
        return currNode;
    }
}