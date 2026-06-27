package dev.vepo.jsonata.functions;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * Thread-local parent stack and path-scoped variable bindings (#$var, @$var).
 */
public final class PathBindings {

    private static final ThreadLocal<Deque<Data>> PARENTS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<Map<String, Data>>> BINDING_SCOPES =
            ThreadLocal.withInitial(PathBindings::newScopeStack);

    private PathBindings() {
    }

    private static Deque<Map<String, Data>> newScopeStack() {
        var stack = new ArrayDeque<Map<String, Data>>();
        stack.push(new HashMap<>());
        return stack;
    }

    public static void pushParent(Data parent) {
        PARENTS.get().push(parent);
    }

    public static void popParent() {
        var stack = PARENTS.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public static Optional<Data> parent(int levels) {
        var stack = PARENTS.get();
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

    public static void pushScope() {
        BINDING_SCOPES.get().push(new HashMap<>());
    }

    public static void popScope() {
        var stack = BINDING_SCOPES.get();
        if (stack.size() > 1) {
            stack.pop();
        }
    }

    public static void bind(String name, Data value) {
        BINDING_SCOPES.get().peekFirst().put(name, value);
    }

    public static void bindIndex(String name, int index) {
        bind(name, JsonFactory.numberValue(index));
    }

    public static Optional<Data> binding(String name) {
        for (var scope : BINDING_SCOPES.get()) {
            var value = scope.get(name);
            if (value != null) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    public static void removeBinding(String name) {
        BINDING_SCOPES.get().peekFirst().remove(name);
    }

    public static void clearBindings() {
        BINDING_SCOPES.set(newScopeStack());
    }

    public static void clearParents() {
        PARENTS.get().clear();
    }
}
