package dev.vepo.jsonata;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.BlockContext;
import dev.vepo.jsonata.functions.DeclaredFunction;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.DataInspector;
import dev.vepo.jsonata.functions.data.DataInspectors;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * External bindings and registered functions for expression evaluation.
 */
public final class EvaluationEnvironment {

    private final Map<String, JsonNode> bindings;
    private final Map<String, Function<MappingCall, Data>> functions;
    private final DataInspector dataInspector;
    private final Guardrails guardrails;

    private EvaluationEnvironment(Map<String, JsonNode> bindings,
                                  Map<String, Function<MappingCall, Data>> functions,
                                  DataInspector dataInspector,
                                  Guardrails guardrails) {
        this.bindings = Map.copyOf(bindings);
        this.functions = Map.copyOf(functions);
        this.dataInspector = dataInspector;
        this.guardrails = guardrails;
    }

    public static EvaluationEnvironment empty() {
        JsonFactory.bootstrap();
        return new EvaluationEnvironment(Map.of(), Map.of(), DataInspectors.defaultInspector(), Guardrails.none());
    }

    public Map<String, JsonNode> bindings() {
        return bindings;
    }

    public Map<String, Function<MappingCall, Data>> functions() {
        return functions;
    }

    public DataInspector dataInspector() {
        return dataInspector;
    }

    public Guardrails guardrails() {
        return guardrails;
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
        return new EvaluationEnvironment(next, functions, dataInspector, guardrails);
    }

    public EvaluationEnvironment registerFunction(String name, Function<MappingCall, Data> implementation) {
        var fnName = name.startsWith("$") ? name : "$" + name;
        var next = new HashMap<>(functions);
        next.put(fnName, implementation);
        return new EvaluationEnvironment(bindings, next, dataInspector, guardrails);
    }

    public EvaluationEnvironment withDataInspector(DataInspector inspector) {
        return new EvaluationEnvironment(bindings, functions, inspector, guardrails);
    }

    public EvaluationEnvironment withGuardrails(Guardrails guardrails) {
        return new EvaluationEnvironment(bindings, functions, dataInspector, guardrails);
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
        private DataInspector dataInspector;
        private Guardrails guardrails = Guardrails.none();

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

        public Builder dataInspector(DataInspector inspector) {
            this.dataInspector = inspector;
            return this;
        }

        public Builder guardrails(Guardrails guardrails) {
            this.guardrails = guardrails;
            return this;
        }

        public EvaluationEnvironment build() {
            var inspector = dataInspector != null ? dataInspector : resolvedDefaultInspector();
            return new EvaluationEnvironment(bindings, functions, inspector, guardrails);
        }

        private static DataInspector resolvedDefaultInspector() {
            JsonFactory.bootstrap();
            return DataInspectors.defaultInspector();
        }
    }
}
