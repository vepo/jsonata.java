package dev.vepo.jsonata.results;

import java.util.List;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.vepo.jsonata.JSONataResult;

/**
 * {@link JSONataResult} backed by a Jackson {@link ArrayNode}.
 * <p>
 * Layer: <strong>domain</strong>. Created via {@link JSONataResults#array(ArrayNode)}.
 * {@link #multi()} exposes one list entry per array element; scalar accessors use the array node
 * as a single Jackson value (typically for array-as-primary results).
 */
class JSONataArrayResult implements JSONataResult {

    private final ArrayNode element;

    JSONataArrayResult(ArrayNode element) {
        this.element = element;
    }

    @Override
    public String asText() {
        return element.toString();
    }

    @Override
    public int asInt() {
        return element.asInt();
    }

    @Override
    public boolean isInt() {
        return false;
    }

    @Override
    public boolean asBoolean() {
        return element.asBoolean();
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
        return element.isEmpty();
    }

    @Override
    public Multi multi() {
        return new Multi() {

            @Override
            public List<String> asText() {
                return IntStream.range(0, element.size())
                                .mapToObj(element::get)
                                .map(JSONataResults::serialize)
                                .toList();
            }

            @Override
            public List<Integer> asInt() {
                return IntStream.range(0, element.size())
                                .mapToObj(element::get)
                                .map(JsonNode::asInt)
                                .toList();
            }

            @Override
            public List<Double> asDouble() {
                return IntStream.range(0, element.size())
                                .mapToObj(element::get)
                                .map(JsonNode::asDouble)
                                .toList();
            }

            @Override
            public List<Boolean> asBoolean() {
                return IntStream.range(0, element.size())
                                .mapToObj(element::get)
                                .map(JsonNode::asBoolean)
                                .toList();
            }

        };
    }
}
