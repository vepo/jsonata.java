package dev.vepo.jsonata.functions.builtin;

import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record TypeOf(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

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
