package dev.vepo.jsonata.functions;

import java.util.ArrayList;
import java.util.LinkedList;

import dev.vepo.jsonata.functions.data.GroupedData;
import dev.vepo.jsonata.functions.data.Data;

public record DeepFind() implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var availableNodes = new LinkedList<Data>();
        var allNodes = new ArrayList<Data>();
        availableNodes.add(current);
        while (!availableNodes.isEmpty()) {
            var currNode = availableNodes.pollFirst();
            if (currNode.isEmpty()) {                    
                continue;
            }            
            allNodes.add(currNode);
            currNode.all().forEachChild(availableNodes::add);
        }
        if (!allNodes.isEmpty()) {
            return new GroupedData(allNodes);
        } else {
            return Mapping.empty();
        }
    }
}