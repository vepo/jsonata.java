package dev.vepo.jsonata.functions;

import static java.util.Spliterators.spliteratorUnknownSize;

import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;

public record MappingJoin(Mapping first, Mapping second) implements Mapping {
    private static final Logger logger = LoggerFactory.getLogger(MappingJoin.class);

    @Override
    public Data map(Data original, Data current) {
        logger.atInfo().log("MappingJoin: first={} second={} current={}", first, second, current);
        var value = first.map(original, current);
        Data result;
        if ((value.isArray() || value.isList()) && !(second instanceof ArrayConstructor)) {
            // && !(second instanceof ObjectBuilder)
            // && !(second instanceof ObjectMapper)) {
            result = new GroupedData(value.stream()
                                          .map(v -> second.map(original, v))
                                          .flatMap(Data::stream)
                                          .filter(Predicate.not(Data::isEmpty))
                                          .toList());
        } else {
            result = second.map(original, value);
        }
        logger.atInfo().log("MappingJoin: result={}", value);
        return result;
    }
}
