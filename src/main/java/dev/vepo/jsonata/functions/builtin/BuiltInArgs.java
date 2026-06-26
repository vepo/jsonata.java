package dev.vepo.jsonata.functions.builtin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;

/**
 * Resolves built-in function arguments with optional context-default semantics.
 */
public final class BuiltInArgs {

    private BuiltInArgs() {
    }

    public static List<Data> evaluate(List<Mapping> providers, int minArgs, int maxArgs,
                                      boolean contextDefault, Data original, Data current) {
        if (contextDefault && providers.isEmpty()) {
            return List.of(current);
        }
        if (providers.size() < minArgs || providers.size() > maxArgs) {
            throw new IllegalArgumentException(
                    "Expected %d-%d arguments but got %d".formatted(minArgs, maxArgs, providers.size()));
        }
        var args = new ArrayList<Data>(providers.size());
        for (var provider : providers) {
            args.add(provider.map(original, current));
        }
        return args;
    }

    public static Data evaluateOne(List<Mapping> providers, Data original, Data current) {
        return evaluate(providers, 0, 1, true, original, current).get(0);
    }

    public static Data evaluateRequired(List<Mapping> providers, int count, Data original, Data current) {
        return evaluate(providers, count, count, false, original, current).get(0);
    }

    public static List<Data> evaluateAll(List<Mapping> providers, int min, int max,
                                         Data original, Data current) {
        return evaluate(providers, min, max, false, original, current);
    }

    public static Function<List<Mapping>, String> arityError(String fn, int min, int max) {
        return providers -> {
            if (providers.isEmpty()) {
                return fn + " requires context or " + min + " argument(s)";
            }
            return fn + " must have " + (min == max ? min : min + " to " + max) + " argument(s)!";
        };
    }
}
