package dev.vepo.jsonata.functions;

import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record BuiltInSumJSONataFunction(FieldPathJSONataFunction extractor) implements JSONataFunction {

    @Override
    public Data map(Data original, Data current) {
        var sumValues = extractor.map(original, current);
        if (!sumValues.isArray()) {
            return JSONataFunction.empty();
        }

        return JsonFactory.numberValue(IntStream.range(0, sumValues.length())
                                                .mapToObj(sumValues::at)
                                                .map(Data::toJson)
                                                .filter(JsonNode::isNumber)
                                                .mapToDouble(JsonNode::asDouble)
                                                .sum());
    }

}