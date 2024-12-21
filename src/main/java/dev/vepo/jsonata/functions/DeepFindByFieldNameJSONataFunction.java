package dev.vepo.jsonata.functions;

import java.util.ArrayList;
import java.util.LinkedList;

import dev.vepo.jsonata.functions.data.GroupedData;
import dev.vepo.jsonata.functions.data.Data;

public record DeepFindByFieldNameJSONataFunction(String fieldName) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        var availableNodes = new LinkedList<Data>();
        var matchedNodes = new ArrayList<Data>();
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
            return new GroupedData(matchedNodes).get(fieldName);
        } else {
            return JSONataFunction.empty();
        }
    }
}