package dev.vepo.jsonata.functions.builtin;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata built-in {@code $encodeUrlComponent}. Encodes a string for use as a URI component. Uses context as the argument when none is supplied.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record EncodeUrlComponent(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var arg = BuiltInArgs.evaluateOne(providers, original, current);
        return JsonFactory.stringValue(URLEncoder.encode(arg.toJson().asText(), StandardCharsets.UTF_8)
                                                 .replace("+", "%20"));
    }
}
