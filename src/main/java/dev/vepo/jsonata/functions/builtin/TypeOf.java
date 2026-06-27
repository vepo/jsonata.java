package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata built-in {@code $type}. Returns the JSONata type name of a value. Uses context when none is supplied.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record TypeOf(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var arg = BuiltInArgs.evaluateOne(providers, original, current);
        if (BuiltInHelper.isUndefined(arg)) {
            return Mapping.empty();
        }
        var json = arg.toJson();
        if (json.isNull()) {
            return JsonFactory.stringValue("null");
        }
        if (json.isTextual()) {
            return JsonFactory.stringValue("string");
        }
        if (json.isNumber()) {
            return JsonFactory.stringValue("number");
        }
        if (json.isBoolean()) {
            return JsonFactory.stringValue("boolean");
        }
        if (arg.isArray() || arg.isList()) {
            return JsonFactory.stringValue("array");
        }
        if (json.isObject()) {
            return JsonFactory.stringValue("object");
        }
        if (arg.isRegex()) {
            return JsonFactory.stringValue("function");
        }
        return Mapping.empty();
    }
}
