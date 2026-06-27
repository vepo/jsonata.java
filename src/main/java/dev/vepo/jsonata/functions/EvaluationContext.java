package dev.vepo.jsonata.functions;

import java.util.concurrent.Callable;

import dev.vepo.jsonata.functions.data.DataInspector;
import dev.vepo.jsonata.functions.data.DataInspectors;

public final class EvaluationContext {

    private static final ThreadLocal<DataInspector> CURRENT = new ThreadLocal<>();

    private EvaluationContext() {}

    public static DataInspector currentInspector() {
        var inspector = CURRENT.get();
        return inspector != null ? inspector : DataInspectors.defaultInspector();
    }

    public static void run(DataInspector inspector, Runnable action) {
        var previous = CURRENT.get();
        CURRENT.set(inspector);
        try {
            action.run();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    public static <T> T call(DataInspector inspector, Callable<T> action) {
        var previous = CURRENT.get();
        CURRENT.set(inspector);
        try {
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
