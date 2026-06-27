package dev.vepo.jsonata.functions.builtin;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * JSONata built-in {@code $formatNumber}. Formats a number according to a decimal picture string.
 *
 * @param providers argument expression mappings from the parse tree
 * @param declaredFunctions function-valued parameters from the parse tree
 */
public record FormatNumber(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var args = BuiltInArgs.evaluate(providers, 1, 3, false, original, current);
        var num = BuiltInHelper.toNumber(args.get(0));
        if (num == null) {
            return Mapping.empty();
        }
        var picture = args.get(1).toJson().asText();
        var format = new DecimalFormat(picture.replace('9', '#'));
        if (args.size() == 3) {
            var options = args.get(2).toJson();
            if (options.has("roundingMode")) {
                format.setRoundingMode(parseRoundingMode(options.get("roundingMode").asText()));
            }
        }
        return JsonFactory.stringValue(format.format(num));
    }

    private static RoundingMode parseRoundingMode(String mode) {
        return switch (mode) {
            case "down" -> RoundingMode.DOWN;
            case "up" -> RoundingMode.UP;
            case "half-up" -> RoundingMode.HALF_UP;
            case "half-down" -> RoundingMode.HALF_DOWN;
            case "half-even" -> RoundingMode.HALF_EVEN;
            case "ceiling" -> RoundingMode.CEILING;
            case "floor" -> RoundingMode.FLOOR;
            default -> RoundingMode.HALF_UP;
        };
    }
}
