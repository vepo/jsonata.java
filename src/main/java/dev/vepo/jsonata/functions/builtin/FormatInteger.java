package dev.vepo.jsonata.functions.builtin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

public record FormatInteger(List<Mapping> providers, List<DeclaredFunction> declaredFunctions) implements Mapping {

    @Override
    public Data map(Data original, Data current) {
        var args = BuiltInArgs.evaluate(providers, 2, 2, false, original, current);
        if (BuiltInHelper.isUndefined(args.get(0))) {
            return Mapping.empty();
        }
        var num = BuiltInHelper.toNumber(args.get(0));
        if (num == null) {
            return Mapping.empty();
        }
        var picture = args.get(1).toJson().asText();
        return JsonFactory.stringValue(format(num, picture));
    }

    static String format(BigDecimal num, String picture) {
        var parts = picture.split(";", 2);
        var digitPattern = parts[0];
        var modifier = parts.length > 1 ? parts[1] : "";
        var rounded = num.setScale(0, RoundingMode.FLOOR);
        var negative = rounded.compareTo(BigDecimal.ZERO) < 0;
        var abs = rounded.abs();
        var digits = abs.toPlainString();
        var minDigits = digitPattern.chars().filter(c -> c == '0').count();
        while (digits.length() < minDigits) {
            digits = "0" + digits;
        }
        if (digitPattern.contains("#")) {
            var hashCount = digitPattern.chars().filter(c -> c == '#').count();
            var totalWidth = hashCount + minDigits;
            while (digits.length() < totalWidth) {
                digits = "0" + digits;
            }
        }
        var result = digits;
        if ("o".equals(modifier)) {
            result = toOrdinal(abs.intValue());
        }
        return negative ? "-" + result : result;
    }

    private static String toOrdinal(int n) {
        var mod100 = n % 100;
        if (mod100 >= 11 && mod100 <= 13) {
            return n + "th";
        }
        return switch (n % 10) {
            case 1 -> n + "st";
            case 2 -> n + "nd";
            case 3 -> n + "rd";
            default -> n + "th";
        };
    }
}
