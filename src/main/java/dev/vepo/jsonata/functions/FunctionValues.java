package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.FunctionData;

public final class FunctionValues {

    private FunctionValues() {
    }

    public static boolean isFunction(Data data) {
        return data instanceof FunctionData;
    }

    public static FunctionData wrap(FunctionValue value) {
        return new FunctionData(value);
    }

    public static FunctionData wrap(PartialApplication partial) {
        return new FunctionData(partial);
    }

    public static FunctionValue asFunctionValue(Data data) {
        if (data instanceof FunctionData fd && fd.isFunctionValue()) {
            return fd.asFunctionValue();
        }
        throw new dev.vepo.jsonata.exception.JSONataException("T1008", "Attempted to invoke a non-function");
    }
}
