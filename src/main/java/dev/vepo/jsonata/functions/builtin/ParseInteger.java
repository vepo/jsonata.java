package dev.vepo.jsonata.functions.builtin;

import java.math.BigDecimal;
import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record ParseInteger(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var args = BuiltInArgs.evaluate(providers, 2, 2, false, original, current);
        var value = args.get(0).toJson().asText();
        var picture = args.get(1).toJson().asText();
        var negative = value.startsWith("-");
        if (negative) {
            value = value.substring(1);
        }
        var digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return Mapping.empty();
        }
        var num = new BigDecimal(digits);
        if (negative) {
            num = num.negate();
        }
        if (picture.contains(";o")) {
            // ordinal input - strip suffix handled above
        }
        return JsonFactory.numberValue(num);
    }
}
