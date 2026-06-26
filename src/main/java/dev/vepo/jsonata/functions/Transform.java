package dev.vepo.jsonata.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.GroupedData;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record Transform(Mapping pattern, Mapping update, Optional<Mapping> delete) implements Mapping {

    @Override
    public Data map(Data original, Data objectToTransform) {
        if (isUndefined(objectToTransform)) {
            return Mapping.empty();
        }
        var result = deepCopy(objectToTransform);
        var matches = pattern.map(original, result);
        if (isUndefined(matches)) {
            return result;
        }
        var matchList = toList(matches);
        for (var match : matchList) {
            applyUpdate(original, match);
            delete.ifPresent(del -> applyDelete(original, match, del));
        }
        return result;
    }

    private void applyUpdate(Data original, Data match) {
        var updateValue = update.map(original, match);
        if (isUndefined(updateValue) || !updateValue.isObject()) {
            return;
        }
        var targetNode = match.toJson();
        if (!(targetNode instanceof ObjectNode objectNode)) {
            throw new JSONataException("Transform update target must be an object");
        }
        updateValue.toJson().fields().forEachRemaining(entry -> objectNode.set(entry.getKey(), entry.getValue()));
    }

    private void applyDelete(Data original, Data match, Mapping deleteMapping) {
        var deletion = deleteMapping.map(original, match);
        if (isUndefined(deletion)) {
            return;
        }
        var names = deletionNames(deletion);
        var targetNode = match.toJson();
        if (targetNode instanceof ObjectNode objectNode) {
            names.forEach(objectNode::remove);
        }
    }

    private static List<String> deletionNames(Data deletion) {
        var names = new ArrayList<String>();
        if (deletion.isArray() || deletion.isList()) {
            for (int i = 0; i < deletion.length(); i++) {
                addDeletionName(names, deletion.at(i));
            }
        } else {
            addDeletionName(names, deletion);
        }
        return names;
    }

    private static void addDeletionName(List<String> names, Data value) {
        var json = value.toJson();
        if (json != null && json.isTextual()) {
            names.add(json.asText());
        }
    }

    private static List<Data> toList(Data data) {
        if (data.isArray() || data.isList()) {
            return data.stream().toList();
        }
        return List.of(data);
    }

    private static Data deepCopy(Data data) {
        if (isUndefined(data)) {
            return Mapping.empty();
        }
        return JsonFactory.fromString(data.toJson().toString());
    }

    private static boolean isUndefined(Data data) {
        return data == null || data.isEmpty();
    }
}
