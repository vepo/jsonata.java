package dev.vepo.jsonata.functions.buildin;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.JSONataAggregateFunction;
import dev.vepo.jsonata.functions.JSONataFunction;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record SumJSONataFunction(List<JSONataFunction> providers,
                                 List<DeclaredFunction> declaredFunctions)
        implements JSONataAggregateFunction {
    private static final Logger logger = LoggerFactory.getLogger(SumJSONataFunction.class);

    public SumJSONataFunction {
        if (providers.size() != 1) {
            throw new IllegalArgumentException("$sum function must have 1 argument");
        }
    }

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

    @Override
    public JSONataFunction extractor() {
        return providers.get(0);
    }
}