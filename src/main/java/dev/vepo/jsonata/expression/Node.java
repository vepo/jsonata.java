package dev.vepo.jsonata.expression;

import java.util.List;

public interface Node {

    String asText();

    int asInt();

    boolean asBoolean();

    boolean isNull();

    boolean isEmpty();

    Multi multi();

    public interface Multi {
        List<String> asText();

        List<Integer> asInt();

        List<Boolean> asBoolean();

    }
}
