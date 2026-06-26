package dev.vepo.jsonata.functions.builtin;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record DecodeUrlComponent(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var arg = BuiltInArgs.evaluateOne(providers, original, current);
        return JsonFactory.stringValue(URLDecoder.decode(arg.toJson().asText(), StandardCharsets.UTF_8));
    }
}
