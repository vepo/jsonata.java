package dev.vepo.jsonata.functions.builtin;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record EncodeUrlComponent(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var arg = BuiltInArgs.evaluateOne(providers, original, current);
        return JsonFactory.stringValue(URLEncoder.encode(arg.toJson().asText(), StandardCharsets.UTF_8)
                                                 .replace("+", "%20"));
    }
}
