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
    private static final ThreadLocal<Map<String, Data>> BINDINGS = ThreadLocal.withInitial(HashMap::new);

    private PathBindings() {
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

    public static void bind(String name, Data value) {
        BINDINGS.get().put(name, value);
    }

    public static void bindIndex(String name, int index) {
        bind(name, JsonFactory.numberValue(index));
    }

    public static Optional<Data> binding(String name) {
        return Optional.ofNullable(BINDINGS.get().get(name));
    }

    public static void removeBinding(String name) {
        BINDINGS.get().remove(name);
    }

    public static void clearBindings() {
        BINDINGS.get().clear();
    }

    public static void clearParents() {
        PARENTS.get().clear();
    }
}
