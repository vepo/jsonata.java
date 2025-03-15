package dev.vepo.jsonata.functions;

import java.util.ArrayList;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;

public record ArrayQueryJSONataFunction(JSONataFunction mapFunction, JSONataFunction filterFunction) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        var mapped = mapFunction.map(original, current);
        if (mapped.isArray() || mapped.isList()) {
            var filteredData = new ArrayList<Data>();
            for (int i = 0; i < mapped.length(); ++i) {
                var currData = mapped.at(i);
                var currResult = filterFunction.map(original, currData).toJson();
                if (currResult.isBoolean() && currResult.asBoolean()) {
                    filteredData.add(current.at(i));
                }
            }
            return new GroupedData(filteredData);
        } else {
            return JSONataFunction.empty();
        }
    }

}
