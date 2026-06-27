package dev.vepo.jsonata.functions.json;

import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.EvaluationContext;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.DataInspector;
import dev.vepo.jsonata.functions.data.EmptyData;

public final class MutableJacksonDataInspector implements DataInspector {

    public static final MutableJacksonDataInspector INSTANCE = new MutableJacksonDataInspector();

    private MutableJacksonDataInspector() {}

    @Override
    public boolean mutableValues() {
        return true;
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
        return data.toJson() instanceof ObjectNode;
    }

    @Override
    public void mergeFields(Data target, Data update) {
        var targetNode = target.toJson();
        if (!(targetNode instanceof ObjectNode objectNode)) {
            throw new JSONataException("Transform update target must be an object");
        }
        update.toJson().fields().forEachRemaining(entry -> objectNode.set(entry.getKey(), entry.getValue()));
    }

    @Override
    public void removeFields(Data target, Iterable<String> fieldNames) {
        var targetNode = target.toJson();
        if (targetNode instanceof ObjectNode objectNode) {
            fieldNames.forEach(objectNode::remove);
        }
    }

    @Override
    public Data merged(Data target, Data update) {
        mergeFields(target, update);
        return target;
    }

    @Override
    public Data withoutFields(Data target, Iterable<String> fieldNames) {
        removeFields(target, fieldNames);
        return target;
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
