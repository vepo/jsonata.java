package dev.vepo.jsonata.functions.json;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.EvaluationContext;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.DataInspector;
import dev.vepo.jsonata.functions.data.EmptyData;

public final class ImmutableJacksonDataInspector implements DataInspector {

    public static final ImmutableJacksonDataInspector INSTANCE = new ImmutableJacksonDataInspector();

    private ImmutableJacksonDataInspector() {}

    @Override
    public boolean mutableValues() {
        return false;
    }

    @Override
    public Data copy(Data data) {
        if (data == null || data.isEmpty()) {
            return new EmptyData();
        }
        return wrap(data.toJson().deepCopy());
    }

    @Override
    public boolean isMutableObject(Data data) {
        return false;
    }

    @Override
    public void mergeFields(Data target, Data update) {
        throw new JSONataException("Cannot mutate immutable data");
    }

    @Override
    public void removeFields(Data target, Iterable<String> fieldNames) {
        throw new JSONataException("Cannot mutate immutable data");
    }

    @Override
    public Data merged(Data target, Data update) {
        var targetNode = target.toJson();
        if (!targetNode.isObject()) {
            throw new JSONataException("Transform update target must be an object");
        }
        if (!update.toJson().isObject()) {
            return target;
        }
        var merged = JacksonDataPaths.deepCopyObjectFields(targetNode);
        JacksonDataPaths.mergeIntoCopy(merged, update.toJson());
        return wrap(merged);
    }

    @Override
    public Data withoutFields(Data target, Iterable<String> fieldNames) {
        var targetNode = target.toJson();
        if (!targetNode.isObject()) {
            return target;
        }
        var copy = JacksonDataPaths.deepCopyObjectFields(targetNode);
        JacksonDataPaths.removeFromCopy(copy, fieldNames);
        return wrap(copy);
    }

    @Override
    public Data replaceNode(Data root, Data current, Data replacement) {
        if (root.toJson() == current.toJson()) {
            return wrap(replacement.toJson().deepCopy());
        }
        var path = JacksonDataPaths.findPath(root.toJson(), current.toJson())
                                   .orElseThrow(() -> new JSONataException("Cannot replace node outside root tree"));
        return wrap(JacksonDataPaths.replaceAt(root.toJson(), path, replacement.toJson()));
    }

    private static Data wrap(com.fasterxml.jackson.databind.JsonNode node) {
        return JsonFactory.json2Value(node, EvaluationContext.currentInspector());
    }
}
