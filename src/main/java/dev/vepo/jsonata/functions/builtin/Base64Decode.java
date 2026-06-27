package dev.vepo.jsonata.functions.builtin;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata built-in {@code $base64decode}. Decodes a Base64-encoded string. Uses context as the argument when none is supplied.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record Base64Decode(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var arg = BuiltInArgs.evaluateOne(providers, original, current);
        var text = arg.toJson().asText();
        return JsonFactory.stringValue(new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8));
    }
}
