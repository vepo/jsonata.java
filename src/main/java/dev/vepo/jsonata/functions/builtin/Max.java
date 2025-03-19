package dev.vepo.jsonata.functions.builtin;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.AggregateMapping;
import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Max(List<Mapping> providers,
                  List<DeclaredFunction> declaredFunctions)
        implements AggregateMapping {

    private static final Logger logger = LoggerFactory.getLogger(Sum.class);

    public Max
    {
        if (providers.size() != 1) {
            throw new IllegalArgumentException("$max function must have 1 argument");
        }
    }

    @Override
    public Data operation(Data maxValues) {
        logger.atDebug().log("Executing max {}", maxValues);
        if (maxValues.isArray() || maxValues.isList()) {
            return IntStream.range(0, maxValues.length())
                            .mapToObj(maxValues::at)
                            .map(Data::toJson)
                            .map(JsonNode::decimalValue)
                            .max(BigDecimal::compareTo)
                            .map(JsonFactory::numberValue)
                            .orElseGet(Mapping::empty);
        } else if (!maxValues.isEmpty() && maxValues.toJson().isNumber()) {
            return maxValues;
        } else {
            return Mapping.empty();
        }
    }

    @Override
    public Mapping extractor() {
        return providers.get(0);
    }
}