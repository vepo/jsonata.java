package dev.vepo.jsonata.functions;

import java.util.concurrent.Callable;

import dev.vepo.jsonata.Guardrails;
import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.data.DataInspector;
import dev.vepo.jsonata.functions.data.DataInspectors;

/**
 * Thread-local evaluation state: data inspector, guardrails, and recursion depth.
 *
 * <p>Domain evaluation runs inside a {@link #call} scope established by the application
 * facade ({@code JSONata}). Nested {@link Mapping#map} invocations inherit the same
 * inspector and guardrails; {@link #callWithDepth} increments depth for stack-limit
 * enforcement.
 *
 * <p>When no scope is active, {@link #currentInspector()} falls back to the default
 * inspector and {@link #currentGuardrails()} to {@link Guardrails#none()}.
 *
 * <p>Not thread-safe across threads — each evaluation thread must use its own scope.
 */
public final class EvaluationContext {

    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    private EvaluationContext() {}

    /**
     * Immutable snapshot of per-evaluation thread-local state.
     *
     * @param inspector   the {@link DataInspector} used for copy/mutate decisions
     * @param guardrails  optional timeout, stack-depth, and sequence-length limits
     * @param startTimeMs wall-clock start for timeout checks
     * @param depth       current recursion depth for stack-limit checks
     */
    public record State(DataInspector inspector, Guardrails guardrails, long startTimeMs, int depth) {
        static State of(DataInspector inspector, Guardrails guardrails) {
            return new State(inspector, guardrails, System.currentTimeMillis(), 0);
        }

        State deeper() {
            return new State(inspector, guardrails, startTimeMs, depth + 1);
        }
    }

    /**
     * Returns the active {@link DataInspector}, or the default when no scope is set.
     *
     * @return the current inspector
     */
    public static DataInspector currentInspector() {
        var state = CURRENT.get();
        return state != null ? state.inspector() : DataInspectors.defaultInspector();
    }

    /**
     * Returns the active {@link Guardrails}, or {@link Guardrails#none()} when no scope is set.
     *
     * @return the current guardrails
     */
    public static Guardrails currentGuardrails() {
        var state = CURRENT.get();
        return state != null ? state.guardrails() : Guardrails.none();
    }

    /**
     * Enforces timeout and stack-depth guardrails if a scope is active.
     *
     * @throws JSONataException {@code D1012} when evaluation timeout is exceeded;
     *                          {@code U1001} when stack depth limit is exceeded
     */
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

    /**
     * Enforces the configured maximum sequence length if a scope is active.
     *
     * @param length the sequence size about to be produced or iterated
     * @throws JSONataException {@code D2015} when the length exceeds the configured limit
     */
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

    /**
     * Runs {@code action} in a new evaluation scope with the given inspector and no guardrails.
     *
     * @param inspector the data inspector for this evaluation
     * @param action    the work to perform
     */
    public static void run(DataInspector inspector, Runnable action) {
        call(inspector, Guardrails.none(), () -> {
            action.run();
            return null;
        });
    }

    /**
     * Runs {@code action} in a new evaluation scope with the given inspector and no guardrails.
     *
     * @param inspector the data inspector for this evaluation
     * @param action    the work to perform
     * @param <T>       the result type
     * @return the value returned by {@code action}
     */
    public static <T> T call(DataInspector inspector, Callable<T> action) {
        return call(inspector, Guardrails.none(), action);
    }

    /**
     * Runs {@code action} in a new evaluation scope, restoring any outer scope on exit.
     *
     * @param inspector  the data inspector for this evaluation
     * @param guardrails optional limits (timeout, stack depth, sequence length)
     * @param action     the work to perform
     * @param <T>        the result type
     * @return the value returned by {@code action}
     */
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

    /**
     * Runs {@code action} with incremented recursion depth for stack-limit tracking.
     * When no scope is active, runs {@code action} without depth tracking.
     *
     * @param action the nested evaluation work
     * @param <T>    the result type
     * @return the value returned by {@code action}
     * @throws JSONataException when guardrails are violated before or during execution
     */
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
