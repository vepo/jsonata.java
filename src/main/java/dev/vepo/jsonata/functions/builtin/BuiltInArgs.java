package dev.vepo.jsonata.functions.builtin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;

/**
 * Argument resolution for built-in function call sites.
 * Evaluates parse-tree argument providers and applies optional context-default semantics:
 * when enabled and no providers are given, the evaluation context is used as the sole argument.
 */
public final class BuiltInArgs {

    private BuiltInArgs() {
    }

    /**
     * Evaluates argument providers and validates arity.
     *
     * @param providers argument expression mappings
     * @param minArgs minimum number of arguments (inclusive)
     * @param maxArgs maximum number of arguments (inclusive)
     * @param contextDefault when true and {@code providers} is empty, use {@code current}
     * @param original root input data for the expression
     * @param current evaluation context data
     * @return evaluated argument values
     * @throws IllegalArgumentException when arity is out of range
     */
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

    /**
     * Evaluates zero or one argument, defaulting to context when none is supplied.
     *
     * @param providers argument expression mappings
     * @param original root input data
     * @param current evaluation context data
     * @return the single evaluated argument
     */
    public static Data evaluateOne(List<Mapping> providers, Data original, Data current) {
        return evaluate(providers, 0, 1, true, original, current).get(0);
    }

    /**
     * Evaluates exactly {@code count} required arguments.
     *
     * @param providers argument expression mappings
     * @param count required argument count
     * @param original root input data
     * @param current evaluation context data
     * @return the first evaluated argument
     * @throws IllegalArgumentException when arity does not match
     */
    public static Data evaluateRequired(List<Mapping> providers, int count, Data original, Data current) {
        return evaluate(providers, count, count, false, original, current).get(0);
    }

    /**
     * Evaluates between {@code min} and {@code max} required arguments.
     *
     * @param providers argument expression mappings
     * @param min minimum argument count
     * @param max maximum argument count
     * @param original root input data
     * @param current evaluation context data
     * @return evaluated argument values
     * @throws IllegalArgumentException when arity is out of range
     */
    public static List<Data> evaluateAll(List<Mapping> providers, int min, int max,
                                         Data original, Data current) {
        return evaluate(providers, min, max, false, original, current);
    }

    /**
     * Builds a JSONata-style arity error message supplier for {@code fn}.
     *
     * @param fn built-in name (e.g. {@code "$sum"})
     * @param min minimum allowed argument count
     * @param max maximum allowed argument count
     * @return function that formats an error from the actual provider count
     */
    public static Function<List<Mapping>, String> arityError(String fn, int min, int max) {
        return providers -> {
            if (providers.isEmpty()) {
                return fn + " requires context or " + min + " argument(s)";
            }
            return fn + " must have " + (min == max ? min : min + " to " + max) + " argument(s)!";
        };
    }
}
