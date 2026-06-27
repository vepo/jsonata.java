package dev.vepo.jsonata.functions;

import java.util.concurrent.Callable;

import dev.vepo.jsonata.Guardrails;
import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.data.DataInspector;
import dev.vepo.jsonata.functions.data.DataInspectors;

public final class EvaluationContext {

    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    private EvaluationContext() {}

    public record State(DataInspector inspector, Guardrails guardrails, long startTimeMs, int depth) {
        static State of(DataInspector inspector, Guardrails guardrails) {
            return new State(inspector, guardrails, System.currentTimeMillis(), 0);
        }

        State deeper() {
            return new State(inspector, guardrails, startTimeMs, depth + 1);
        }
    }

    public static DataInspector currentInspector() {
        var state = CURRENT.get();
        return state != null ? state.inspector() : DataInspectors.defaultInspector();
    }

    public static Guardrails currentGuardrails() {
        var state = CURRENT.get();
        return state != null ? state.guardrails() : Guardrails.none();
    }

    public static void checkGuardrails() {
        var state = CURRENT.get();
        if (state == null) {
            return;
        }
        state.guardrails().timeoutMs().ifPresent(timeout -> {
            if (System.currentTimeMillis() - state.startTimeMs() > timeout) {
                throw new JSONataException("D1012", "Evaluation timeout exceeded");
            }
        });
        state.guardrails().maxStackDepth().ifPresent(max -> {
            if (state.depth() > max) {
                throw new JSONataException("U1001", "Stack depth limit exceeded");
            }
        });
    }

    public static void checkSequenceLength(int length) {
        var state = CURRENT.get();
        if (state == null) {
            return;
        }
        state.guardrails().maxSequenceLength().ifPresent(max -> {
            if (length > max) {
                throw new JSONataException("D2015", "Sequence length limit exceeded");
            }
        });
    }

    public static void run(DataInspector inspector, Runnable action) {
        call(inspector, Guardrails.none(), () -> {
            action.run();
            return null;
        });
    }

    public static <T> T call(DataInspector inspector, Callable<T> action) {
        return call(inspector, Guardrails.none(), action);
    }

    public static <T> T call(DataInspector inspector, Guardrails guardrails, Callable<T> action) {
        var previous = CURRENT.get();
        CURRENT.set(State.of(inspector, guardrails));
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

    public static <T> T callWithDepth(Callable<T> action) {
        var state = CURRENT.get();
        if (state == null) {
            try {
                return action.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        checkGuardrails();
        var previous = CURRENT.get();
        CURRENT.set(state.deeper());
        try {
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            CURRENT.set(previous);
        }
    }
}
