package dev.vepo.jsonata.expression;

import static dev.vepo.jsonata.expression.transformers.Value.empty;

import java.util.ArrayList;
import java.util.LinkedList;

import dev.vepo.jsonata.expression.transformers.Value;
import dev.vepo.jsonata.expression.transformers.Value.GroupedValue;

public record DeepFindByFieldNameExpression(String fieldName) implements Expression {

    @Override
    public Value map(Value original, Value current) {
        var availableNodes = new LinkedList<Value>();
        var matchedNodes = new ArrayList<Value>();
        availableNodes.add(current);
        while (!availableNodes.isEmpty()) {
            var currNode = availableNodes.pollFirst();
            if (currNode.isEmpty()) {                    
                continue;
            }

            if (currNode.isObject() && currNode.hasField(fieldName)) {
                matchedNodes.add(currNode);                       
            } else {
                currNode.forEachChild(availableNodes::offerLast);
            }
        }
        if (!matchedNodes.isEmpty()) {
            return new GroupedValue(matchedNodes).get(fieldName);
        } else {
            return empty();
        }
    }
}