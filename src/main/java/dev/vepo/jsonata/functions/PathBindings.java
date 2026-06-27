package dev.vepo.jsonata.functions;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * Thread-local parent stack and path-scoped variable bindings.
 *
 * <p>Supports JSONata path features that require implicit context beyond {@code current}:
 * parent references ({@code %}), positional variables ({@code #$var}), and focus
 * variables ({@code @$var}). Bindings are scoped via push/pop so nested evaluation
 * (array mapping, function invocation) does not leak state.
 *
 * <p>Not thread-safe across threads — each evaluation thread maintains its own stacks.
 * Call {@link #clear()} when evaluation completes so pooled threads do not retain state.
 */
public final class PathBindings {

    private static final ThreadLocal<State> CURRENT = ThreadLocal.withInitial(State::new);

    private PathBindings() {
    }

    private static final class State {
        private final Deque<Data> parents = new ArrayDeque<>();
        private final Deque<Map<String, Data>> bindingScopes = newScopeStack();
    }

    private static Deque<Map<String, Data>> newScopeStack() {
        var stack = new ArrayDeque<Map<String, Data>>();
        stack.push(new HashMap<>());
        return stack;
    }

    private static State state() {
        return CURRENT.get();
    }

    /**
     * Pushes a parent value onto the parent stack for {@code %} references.
     *
     * @param parent the parent context to expose
     */
    public static void pushParent(Data parent) {
        state().parents.push(parent);
    }

    /**
     * Removes the most recently pushed parent from the stack.
     */
    public static void popParent() {
        var stack = state().parents;
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    /**
     * Resolves the parent at the given ancestor level ({@code 1} = immediate parent).
     *
     * @param levels how many levels up the parent stack to traverse
     * @return the parent value, or empty when the stack is too shallow
     */
    public static Optional<Data> parent(int levels) {
        var stack = state().parents;
        if (stack.size() < levels) {
            return Optional.empty();
        }
        var iterator = stack.iterator();
        Data result = null;
        for (int i = 0; i < levels && iterator.hasNext(); i++) {
            result = iterator.next();
        }
        return Optional.ofNullable(result);
    }

    /**
     * Pushes a new binding scope (e.g. at function invocation).
     */
    public static void pushScope() {
        state().bindingScopes.push(new HashMap<>());
    }

    /**
     * Pops the innermost binding scope, preserving at least one scope on the stack.
     */
    public static void popScope() {
        var stack = state().bindingScopes;
        if (stack.size() > 1) {
            stack.pop();
        }
    }

    /**
     * Binds a path-scoped variable name to a value in the current scope.
     *
     * @param name  variable name (without {@code #} or {@code @} prefix)
     * @param value the bound value
     */
    public static void bind(String name, Data value) {
        state().bindingScopes.peekFirst().put(name, value);
    }

    /**
     * Binds a positional index variable to a numeric value.
     *
     * @param name  index variable name
     * @param index the zero-based index
     */
    public static void bindIndex(String name, int index) {
        bind(name, JsonFactory.numberValue(index));
    }

    /**
     * Looks up a path-scoped binding, searching from innermost to outermost scope.
     *
     * @param name the variable name
     * @return the bound value, or empty when not found
     */
    public static Optional<Data> binding(String name) {
        for (var scope : state().bindingScopes) {
            var value = scope.get(name);
            if (value != null) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    /**
     * Removes a binding from the current scope only.
     *
     * @param name the variable name to unbind
     */
    public static void removeBinding(String name) {
        state().bindingScopes.peekFirst().remove(name);
    }

    /**
     * Discards all path binding state for the current thread.
     *
     * <p>Prefer this over clearing individual stacks so pooled threads do not retain
     * {@link ThreadLocal} entries after evaluation.
     */
    public static void clear() {
        CURRENT.remove();
    }

    /** @see #clear() */
    public static void clearBindings() {
        clear();
    }

    /** @see #clear() */
    public static void clearParents() {
        clear();
    }
}
