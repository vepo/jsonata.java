package dev.vepo.jsonata.functions.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class JacksonDataPaths {

    sealed interface Step permits ObjectField, ArrayIndex {
    }

    record ObjectField(String name) implements Step {
    }

    record ArrayIndex(int index) implements Step {
    }

    private JacksonDataPaths() {}

    static Optional<List<Step>> findPath(JsonNode root, JsonNode target) {
        if (root == target) {
            return Optional.of(List.of());
        }
        if (root.isObject()) {
            var fields = root.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                var childPath = findPath(entry.getValue(), target);
                if (childPath.isPresent()) {
                    var path = new ArrayList<Step>();
                    path.add(new ObjectField(entry.getKey()));
                    path.addAll(childPath.get());
                    return Optional.of(path);
                }
            }
        } else if (root.isArray()) {
            for (int i = 0; i < root.size(); i++) {
                var childPath = findPath(root.get(i), target);
                if (childPath.isPresent()) {
                    var path = new ArrayList<Step>();
                    path.add(new ArrayIndex(i));
                    path.addAll(childPath.get());
                    return Optional.of(path);
                }
            }
        }
        return Optional.empty();
    }

    static JsonNode navigate(JsonNode root, List<Step> path) {
        var current = root;
        for (var step : path) {
            current = switch (step) {
                case ObjectField(var name) -> current.get(name);
                case ArrayIndex(var index) -> current.get(index);
            };
        }
        return current;
    }

    static JsonNode replaceAt(JsonNode root, List<Step> path, JsonNode replacement) {
        if (path.isEmpty()) {
            return replacement.deepCopy();
        }
        var copy = root.deepCopy();
        var parent = navigate(copy, path.subList(0, path.size() - 1));
        var last = path.getLast();
        switch (last) {
            case ObjectField(var name) -> ((ObjectNode) parent).set(name, replacement.deepCopy());
            case ArrayIndex(var index) -> ((ArrayNode) parent).set(index, replacement.deepCopy());
        }
        return copy;
    }

    static JsonNode deepCopyObjectFields(JsonNode objectNode) {
        return objectNode.deepCopy();
    }

    static void mergeIntoCopy(JsonNode targetCopy, JsonNode update) {
        if (!(targetCopy instanceof ObjectNode objectNode) || !update.isObject()) {
            return;
        }
        update.fields().forEachRemaining(entry -> objectNode.set(entry.getKey(), entry.getValue()));
    }

    static void removeFromCopy(JsonNode targetCopy, Iterable<String> fieldNames) {
        if (!(targetCopy instanceof ObjectNode objectNode)) {
            return;
        }
        fieldNames.forEach(objectNode::remove);
    }
}
