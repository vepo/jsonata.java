package dev.vepo.jsonata;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.BlockContext;
import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * External bindings and registered functions for expression evaluation.
 */
public final class EvaluationEnvironment {

    private final Map<String, JsonNode> bindings;
    private final Map<String, Function<MappingCall, Data>> functions;

    private EvaluationEnvironment(Map<String, JsonNode> bindings,
                                  Map<String, Function<MappingCall, Data>> functions) {
        this.bindings = Map.copyOf(bindings);
        this.functions = Map.copyOf(functions);
    }

    public static EvaluationEnvironment empty() {
        return new EvaluationEnvironment(Map.of(), Map.of());
    }

    public Map<String, JsonNode> bindings() {
        return bindings;
    }

    public Map<String, Function<MappingCall, Data>> functions() {
        return functions;
    }

    public BlockContext rootBlockContext() {
        var context = new BlockContext(new java.util.LinkedList<>());
        bindings.forEach((name, value) -> {
            var varName = name.startsWith("$") ? name : "$" + name;
            var data = JsonFactory.json2Value(value);
            context.defineVariable(varName, (original, current) -> data);
        });
        return context;
    }

    public EvaluationEnvironment bind(String name, JsonNode value) {
        var key = name.startsWith("$") ? name.substring(1) : name;
        var next = new HashMap<>(bindings);
        next.put(key, value);
        return new EvaluationEnvironment(next, functions);
    }

    public EvaluationEnvironment registerFunction(String name, Function<MappingCall, Data> implementation) {
        var fnName = name.startsWith("$") ? name : "$" + name;
        var next = new HashMap<>(functions);
        next.put(fnName, implementation);
        return new EvaluationEnvironment(bindings, next);
    }

    public static Builder builder() {
        return new Builder();
    }

    public record MappingCall(Data original, Data current, java.util.List<Mapping> arguments,
                              java.util.List<DeclaredFunction> declaredFunctions) {
    }

    public static final class Builder {
        private final Map<String, JsonNode> bindings = new HashMap<>();
        private final Map<String, Function<MappingCall, Data>> functions = new HashMap<>();

        public Builder bind(String name, JsonNode value) {
            var key = name.startsWith("$") ? name.substring(1) : name;
            bindings.put(key, value);
            return this;
        }

        public Builder registerFunction(String name, Function<MappingCall, Data> implementation) {
            var fnName = name.startsWith("$") ? name : "$" + name;
            functions.put(fnName, implementation);
            return this;
        }

        public EvaluationEnvironment build() {
            return new EvaluationEnvironment(bindings, functions);
        }
    }
}
