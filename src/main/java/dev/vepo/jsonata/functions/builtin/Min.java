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

/**
 * JSONata built-in {@code $min}. Returns the minimum numeric value in an array, or the number itself. Uses context when no argument is supplied.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record Min(List<Mapping> providers,
                  List<DeclaredFunction> declaredFunctions)
        implements AggregateMapping {

    private static final Logger logger = LoggerFactory.getLogger(Sum.class);

    /** {@inheritDoc} */
    @Override
    public Data operation(Data minValues) {
        logger.atDebug().log("Executing min {}", minValues);
        if (minValues.isArray() || minValues.isList()) {
            return IntStream.range(0, minValues.length())
                            .mapToObj(minValues::at)
                            .map(Data::toJson)
                            .map(JsonNode::decimalValue)
                            .min(BigDecimal::compareTo)
                            .map(JsonFactory::numberValue)
                            .orElseGet(Mapping::empty);
        } else if (!minValues.isEmpty() && minValues.toJson().isNumber()) {
            return minValues;
        } else {
            return Mapping.empty();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Uses context as the aggregate input when no explicit argument is provided.
     */
    @Override
    public Mapping extractor() {
        return BuiltInHelper.contextExtractor(providers);
    }
}