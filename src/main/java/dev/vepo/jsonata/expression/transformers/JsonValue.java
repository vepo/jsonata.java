package dev.vepo.jsonata.expression.transformers;

import static dev.vepo.jsonata.expression.transformers.ValueFactory.json2Value;
import static java.util.Objects.requireNonNull;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.expression.Expression;
import dev.vepo.jsonata.expression.Node;

public class JsonValue {
    static final ObjectMapper mapper = new ObjectMapper();

    private Value actual;

    public JsonValue(String value) {
        try {
            actual = json2Value(mapper.readTree(value));
        } catch (JsonProcessingException e) {
            throw new JSONataException("Could not load JSON!", e);
        }
    }

    public Node apply(List<Expression> expressions) {
        requireNonNull(expressions, "Expressions cannot be null!");
        return expressions.stream()
                          .reduce((f1, f2) -> (o, v) -> f2.map(o, f1.map(o, v)))
                          .map(f -> f.map(actual, actual)
                                     .toNode())
                          .orElse(actual.toNode());
    }

}
