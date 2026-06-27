package dev.vepo.jsonata.functions.json;

import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.EvaluationContext;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.DataInspector;
import dev.vepo.jsonata.functions.data.EmptyData;

/**
 * Mutable {@link DataInspector} adapter for Jackson-backed {@link Data}.
 * <p>
 * Infrastructure implementation of the domain port: performs in-place updates on
 * underlying {@link ObjectNode} instances. Registered as the default inspector by
 * {@link JsonFactory} static initialization.
 */
public final class MutableJacksonDataInspector implements DataInspector {

    /** Singleton instance used as the default inspector. */
    public static final MutableJacksonDataInspector INSTANCE = new MutableJacksonDataInspector();

    private MutableJacksonDataInspector() {}

    /** {@inheritDoc} */
    @Override
    public boolean mutableValues() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Data copy(Data data) {
        if (data == null || data.isEmpty()) {
            return new EmptyData();
        }
        return wrap(data.toJson().deepCopy());
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMutableObject(Data data) {
        return data.toJson() instanceof ObjectNode;
    }

    /** {@inheritDoc} */
    @Override
    public void mergeFields(Data target, Data update) {
        var targetNode = target.toJson();
        if (!(targetNode instanceof ObjectNode objectNode)) {
            throw new JSONataException("Transform update target must be an object");
        }
        update.toJson().fields().forEachRemaining(entry -> objectNode.set(entry.getKey(), entry.getValue()));
    }

    /** {@inheritDoc} */
    @Override
    public void removeFields(Data target, Iterable<String> fieldNames) {
        var targetNode = target.toJson();
        if (targetNode instanceof ObjectNode objectNode) {
            fieldNames.forEach(objectNode::remove);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Data merged(Data target, Data update) {
        mergeFields(target, update);
        return target;
    }

    /** {@inheritDoc} */
    @Override
    public Data withoutFields(Data target, Iterable<String> fieldNames) {
        removeFields(target, fieldNames);
        return target;
    }

    /** {@inheritDoc} */
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
