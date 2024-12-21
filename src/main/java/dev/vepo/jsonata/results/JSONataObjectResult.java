package dev.vepo.jsonata.results;

import static java.util.Collections.singletonList;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.JSONataResult;

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

        };
    }

}