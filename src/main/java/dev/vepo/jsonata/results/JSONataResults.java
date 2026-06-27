package dev.vepo.jsonata.results;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.vepo.jsonata.JSONataResult;

/**
 * Factory for {@link JSONataResult} implementations produced during evaluation.
 * <p>
 * Layer: <strong>domain</strong>. Called from {@link dev.vepo.jsonata.functions.data.Data#toNode()}
 * and related domain types; not used directly by embedders (prefer {@link dev.vepo.jsonata.JSONata}).
 * <p>
 * Invariants: not instantiable; each factory method returns a view appropriate to the JSONata
 * value shape (empty sequence, scalar/object, array, or grouped sequence).
 */
public abstract class JSONataResults {

    static String serialize(JsonNode node) {
        if (node.isObject()) {
            return node.toString();
        } else {
            return node.asText();
        }
    }

    /**
     * JSONata empty sequence — no value. Scalar accessors on the result throw;
     * {@link JSONataResult#multi()} yields empty lists.
     *
     * @return empty result singleton view
     */
    public static JSONataResult empty() {
        return new JSONataEmptyResult();
    }

    /**
     * Single JSON value (object, array, or scalar) as the primary result.
     *
     * @param element Jackson node representing the value
     * @return result view with singleton {@link JSONataResult#multi()} semantics
     */
    public static JSONataResult object(JsonNode element) {
        return new JSONataObjectResult(element);
    }

    /**
     * JSON array as the primary result; {@link JSONataResult#multi()} lists each element.
     *
     * @param element Jackson array node
     * @return array-backed result view
     */
    public static JSONataResult array(ArrayNode element) {
        return new JSONataArrayResult(element);
    }

    /**
     * Merged sequence from grouped evaluation (e.g. wildcard or flatten paths).
     * Scalar accessors aggregate across members; {@link JSONataResult#multi()} flattens nested sequences.
     *
     * @param elements result views to combine
     * @return grouped result view
     */
    public static JSONataResult group(List<JSONataResult> elements) {
        return new JSONataGroupResult(elements);
    }

    private JSONataResults() {
    }
}
