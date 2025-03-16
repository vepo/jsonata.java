package dev.vepo.jsonata.functions;

import java.math.BigDecimal;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record BuiltInSumJSONataFunction(JSONataFunction extractor) implements JSONataAggregateFunction {
    private static final Logger logger = LoggerFactory.getLogger(BuiltInSumJSONataFunction.class);

    @Override
    public Data operation(Data sumValues) {
        logger.atDebug().log("Executing sum {}", sumValues);
        if (sumValues.isArray() || sumValues.isList()) {
            return JsonFactory.numberValue(IntStream.range(0, sumValues.length())
                                                    .mapToObj(sumValues::at)
                                                    .map(Data::toJson)
                                                    .map(JsonNode::decimalValue)
                                                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        } else if (!sumValues.isEmpty() && sumValues.toJson().isNumber()) {
            return sumValues;
        } else {
            return JSONataFunction.empty();
        }
    }
}