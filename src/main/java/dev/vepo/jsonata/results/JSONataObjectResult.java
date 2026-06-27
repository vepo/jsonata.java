package dev.vepo.jsonata.results;

import static java.util.Collections.singletonList;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.JSONataResult;

/**
 * {@link JSONataResult} backed by a single Jackson {@link JsonNode}.
 * <p>
 * Layer: <strong>domain</strong>. Created via {@link JSONataResults#object(JsonNode)}.
 * {@link #multi()} wraps the scalar in a one-element list; {@link #isEmpty()} is always
 * {@code false} for non-null nodes.
 */
class JSONataObjectResult implements JSONataResult {

    private final JsonNode element;

    JSONataObjectResult(JsonNode element) {
        this.element = element;
    }

    @Override
    public String asText() {
        return JSONataResults.serialize(element);
    }

    @Override
    public int asInt() {
        return element.asInt();
    }

    @Override
    public boolean isInt() {
        return  element.isNumber();
    }

    @Override
    public double asDouble() {
        return element.asDouble();
    }

    @Override
    public boolean isDouble() {
        return element.isDouble();
    }

    @Override
    public boolean isNull() {
        return element.isNull();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean asBoolean() {
        return element.asBoolean();
    }

    @Override
    public Multi multi() {
        return new Multi() {

            @Override
            public List<String> asText() {
                return singletonList(JSONataObjectResult.this.asText());
            }

            @Override
            public List<Integer> asInt() {
                return singletonList(JSONataObjectResult.this.asInt());
            }

            @Override
            public List<Boolean> asBoolean() {
                return singletonList(JSONataObjectResult.this.asBoolean());
            }

            @Override
            public List<Double> asDouble() {
                return singletonList(JSONataObjectResult.this.asDouble());
            }

        };
    }

}
