package dev.vepo.jsonata.results;

import java.util.List;
import java.util.stream.Collectors;

import dev.vepo.jsonata.JSONataResult;

class JSONataGroupResult implements JSONataResult {

    private final List<JSONataResult> elements;

    JSONataGroupResult(List<JSONataResult> elements) {
        this.elements = elements;
    }

    @Override
    public String asText() {
        return elements.stream().map(JSONataResult::asText).collect(Collectors.joining(", "));
    }

    @Override
    public int asInt() {
        return elements.stream().mapToInt(JSONataResult::asInt).sum();
    }

    @Override
    public boolean isInt() {
        return this.elements.stream()
                            .allMatch(JSONataResult::isInt);
    }

    @Override
    public boolean asBoolean() {
        return elements.stream().map(JSONataResult::asBoolean).reduce((b1, b2) -> b1 && b2).orElse(false);
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public Multi multi() {
        return new Multi() {

            @Override
            public List<String> asText() {
                return elements.stream()
                               .flatMap(n -> n.multi().asText().stream())
                               .toList();
            }

            @Override
            public List<Integer> asInt() {
                return elements.stream()
                               .flatMap(n -> n.multi().asInt().stream())
                               .toList();
            }

            @Override
            public List<Boolean> asBoolean() {
                return elements.stream()
                               .flatMap(n -> n.multi().asBoolean().stream())
                               .toList();
            }

        };
    }
}