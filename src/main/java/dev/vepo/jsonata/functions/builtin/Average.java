package dev.vepo.jsonata.functions.builtin;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

public record Average(List<Mapping> providers,
                      List<DeclaredFunction> declaredFunctions)
        implements AggregateMapping {

    private static final Logger logger = LoggerFactory.getLogger(Sum.class);

    public Average {
        if (providers.size() != 1) {
            throw new IllegalArgumentException("$average function must have 1 argument");
        }
    }

    @Override
    public Data operation(Data avgValues) {
        logger.atDebug().log("Executing max {}", avgValues);
        if ((avgValues.isArray() || avgValues.isList()) && avgValues.length() > 0) {
            return JsonFactory.numberValue(IntStream.range(0, avgValues.length())
                                                    .mapToObj(avgValues::at)
                                                    .map(Data::toJson)
                                                    .map(JsonNode::decimalValue)
                                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                                                    .divide(BigDecimal.valueOf(avgValues.length()),12, RoundingMode.HALF_DOWN));
        } else if (!avgValues.isEmpty() && avgValues.toJson().isNumber()) {
            return avgValues;
        } else {
            return Mapping.empty();
        }
    }

    @Override
    public Mapping extractor() {
        return providers.get(0);
    }
}