package dev.vepo.jsonata.results;

import static java.util.Collections.emptyList;

import java.util.List;

import dev.vepo.jsonata.JSONataResult;

class JSONataEmptyResult implements JSONataResult {
    private static RuntimeException emptyValueException() {
        return new IllegalStateException("Value is empty");
    }

    JSONataEmptyResult() {
    }

    @Override
    public String asText() {
        throw emptyValueException();
    }

    @Override
    public int asInt() {
        throw emptyValueException();
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public boolean isInt() {
        return false;
    }

    @Override
    public double asDouble() {
        throw emptyValueException();
    }

    @Override
    public boolean isDouble() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean asBoolean() {
        throw emptyValueException();
    }

    @Override
    public Multi multi() {
        return new Multi() {

            @Override
            public List<String> asText() {
                return emptyList();
            }

            @Override
            public List<Integer> asInt() {
                return emptyList();
            }

            @Override
            public List<Boolean> asBoolean() {
                return emptyList();
            }
        };
    }
}