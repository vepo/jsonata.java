package dev.vepo.jsonata.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.DataInspector;
import dev.vepo.jsonata.functions.json.JacksonDataPaths;

public record Transform(Mapping pattern, Mapping update, Optional<Mapping> delete) implements Mapping {

    @Override
    public Data map(Data original, Data objectToTransform) {
        if (isUndefined(objectToTransform)) {
            return Mapping.empty();
        }
        var inspector = EvaluationContext.currentInspector();
        var result = inspector.copy(objectToTransform);
        var matches = pattern.map(original, result);
        if (isUndefined(matches)) {
            return result;
        }
        var matchList = toList(matches);
        if (inspector.mutableValues()) {
            for (var match : matchList) {
                applyUpdateMutable(original, match, inspector);
                delete.ifPresent(del -> applyDeleteMutable(original, match, del, inspector));
            }
            return result;
        }
        var paths = matchList.stream()
                             .map(match -> JacksonDataPaths.findPathOrThrow(result, match))
                             .toList();
        var currentResult = result;
        for (var path : paths) {
            var match = JacksonDataPaths.navigateData(currentResult, path);
            currentResult = applyUpdateImmutable(original, currentResult, match, inspector);
            if (delete.isPresent()) {
                match = JacksonDataPaths.navigateData(currentResult, path);
                currentResult = applyDeleteImmutable(original, currentResult, match, delete.get(), inspector);
            }
        }
        return currentResult;
    }

    private void applyUpdateMutable(Data original, Data match, DataInspector inspector) {
        var updateValue = update.map(original, match);
        if (isUndefined(updateValue) || !updateValue.isObject()) {
            return;
        }
        inspector.mergeFields(match, updateValue);
    }

    private void applyDeleteMutable(Data original, Data match, Mapping deleteMapping, DataInspector inspector) {
        var deletion = deleteMapping.map(original, match);
        if (isUndefined(deletion)) {
            return;
        }
        inspector.removeFields(match, deletionNames(deletion));
    }

    private Data applyUpdateImmutable(Data original, Data result, Data match, DataInspector inspector) {
        var updateValue = update.map(original, match);
        if (isUndefined(updateValue) || !updateValue.isObject()) {
            return result;
        }
        return inspector.replaceNode(result, match, inspector.merged(match, updateValue));
    }

    private Data applyDeleteImmutable(Data original, Data result, Data match, Mapping deleteMapping,
                                    DataInspector inspector) {
        var deletion = deleteMapping.map(original, match);
        if (isUndefined(deletion)) {
            return result;
        }
        return inspector.replaceNode(result, match, inspector.withoutFields(match, deletionNames(deletion)));
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

    private static boolean isUndefined(Data data) {
        return data == null || data.isEmpty();
    }
}
