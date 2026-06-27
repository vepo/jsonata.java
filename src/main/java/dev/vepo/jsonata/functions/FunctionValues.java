package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.FunctionData;

/**
 * Bridges first-class function values ({@link FunctionValue}, {@link PartialApplication})
 * and their {@link Data} representation at runtime.
 *
 * <p>JSONata functions are not plain JSON; they are carried as {@link FunctionData}
 * wrappers so path expressions and operators can treat them uniformly with other values.
 */
public final class FunctionValues {

    private FunctionValues() {
    }

    /**
     * Tests whether {@code data} wraps a function or partial application.
     *
     * @param data the value to test
     * @return {@code true} when {@code data} is {@link FunctionData}
     */
    public static boolean isFunction(Data data) {
        return data instanceof FunctionData;
    }

    /**
     * Wraps a user-defined function value for use in expression results.
     *
     * @param value the function value
     * @return a {@link FunctionData} carrier
     */
    public static FunctionData wrap(FunctionValue value) {
        return new FunctionData(value);
    }

    /**
     * Wraps a partial application for use in expression results.
     *
     * @param partial the partially applied function
     * @return a {@link FunctionData} carrier
     */
    public static FunctionData wrap(PartialApplication partial) {
        return new FunctionData(partial);
    }

    /**
     * Unwraps a {@link FunctionData} as a {@link FunctionValue}.
     *
     * @param data the value expected to be a function
     * @return the underlying function value
     * @throws dev.vepo.jsonata.exception.JSONataException {@code T1008} when {@code data} is not a function
     */
    public static FunctionValue asFunctionValue(Data data) {
        if (data instanceof FunctionData fd && fd.isFunctionValue()) {
            return fd.asFunctionValue();
        }
        throw new dev.vepo.jsonata.exception.JSONataException("T1008", "Attempted to invoke a non-function");
    }
}
