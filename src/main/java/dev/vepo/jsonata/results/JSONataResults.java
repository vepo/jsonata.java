package dev.vepo.jsonata.results;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.vepo.jsonata.JSONataResult;

public abstract class JSONataResults {

    static String serialize(JsonNode node) {
        if (node.isObject()) {
            return node.toString();
        } else {
            return node.asText();
        }
    }

    public static JSONataResult empty() {
        return new JSONataEmptyResult();
    }

    public static JSONataResult object(JsonNode element) {
        return new JSONataObjectResult(element);
    }

    public static JSONataResult array(ArrayNode element) {
        return new JSONataArrayResult(element);
    }

    public static JSONataResult group(List<JSONataResult> elements) {
        return new JSONataGroupResult(elements);
    }

    private JSONataResults() {
    }
}
