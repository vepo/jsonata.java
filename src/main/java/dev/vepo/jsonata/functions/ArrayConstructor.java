package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.arrayNode;
import static dev.vepo.jsonata.functions.json.JsonFactory.json2Value;

import java.util.ArrayList;
import java.util.List;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;

public record ArrayConstructor(List<Mapping> arrayBuilder) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        if (current.isArray() && arrayBuilder.size() == 1) {
            var elements = new ArrayList<Data>();
            for (int i = 0; i < current.length(); ++i) {
                elements.add(arrayBuilder.get(0).map(original, current.at(i)));
            }
            return json2Value(new GroupedData(elements).toJson());
        } else {
            return json2Value(arrayNode(arrayBuilder.stream().map(fn -> fn.map(original, current).toJson()).toList()));
        }
    }
}