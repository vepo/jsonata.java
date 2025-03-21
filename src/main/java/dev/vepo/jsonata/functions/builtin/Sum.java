package dev.vepo.jsonata.functions.builtin;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.AggregateMapping;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Sum(List<Mapping> providers,
                  List<DeclaredFunction> declaredFunctions)
        implements AggregateMapping {
    private static final Logger logger = LoggerFactory.getLogger(Sum.class);

    public Sum {
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
            return Mapping.empty();
        }
    }

    @Override
    public Mapping extractor() {
        return providers.get(0);
    }
}