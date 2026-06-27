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
 * External bindings, registered host functions, and evaluation options for JSONata.
 * <p>
 * Layer: <strong>application</strong>. Passed to {@link JSONata#jsonata(String, EvaluationEnvironment)}
 * at compile time and carried through {@link JSONata#bind} / {@link JSONata#registerFunction}.
 * Immutable-style: mutating methods return new instances.
 * <p>
 * Usage pattern:
 * <pre>{@code
 * var env = EvaluationEnvironment.builder()
 *     .bind("threshold", JsonFactory.fromString("100").toJson())
 *     .registerFunction("lookup", call -> ...)
 *     .build();
 * var j = JSONata.jsonata("$lookup($threshold)", env);
 * }</pre>
 * <p>
 * Invariants: binding keys are stored without a leading {@code $}; function names are stored
 * with a leading {@code $}. Maps exposed by accessors are unmodifiable copies.
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

    /**
     * Environment with no bindings, no registered functions, default {@link DataInspector}, and no guardrails.
     * Bootstraps {@link JsonFactory} on first use.
     *
     * @return empty environment suitable as a starting point
     */
    public static EvaluationEnvironment empty() {
        JsonFactory.bootstrap();
        return new EvaluationEnvironment(Map.of(), Map.of(), DataInspectors.defaultInspector(), Guardrails.none());
    }

    /**
     * External variable bindings supplied at compile time (keys without {@code $} prefix).
     *
     * @return unmodifiable map of name to JSON value
     */
    public Map<String, JsonNode> bindings() {
        return bindings;
    }

    /**
     * Host functions registered for expression calls (keys with {@code $} prefix).
     *
     * @return unmodifiable map of function name to implementation
     */
    public Map<String, Function<MappingCall, Data>> functions() {
        return functions;
    }

    /**
     * Backing-store adapter applied during evaluation via {@link dev.vepo.jsonata.functions.EvaluationContext}.
     *
     * @return active data inspector
     */
    public DataInspector dataInspector() {
        return dataInspector;
    }

    /**
     * Resource limits enforced during evaluation (timeout, stack depth, sequence length).
     *
     * @return guardrail configuration; {@link Guardrails#none()} when unset
     */
    public Guardrails guardrails() {
        return guardrails;
    }

    /**
     * Block scope pre-populated with external bindings for expression evaluation.
     * Variable names are normalized to include a leading {@code $}.
     *
     * @return root block context for the mapping pipeline
     */
    public BlockContext rootBlockContext() {
        var context = new BlockContext(new java.util.LinkedList<>());
        bindings.forEach((name, value) -> {
            var varName = name.startsWith("$") ? name : "$" + name;
            var data = JsonFactory.json2Value(value);
            context.defineVariable(varName, (original, current) -> data);
        });
        return context;
    }

    /**
     * Returns a copy with an additional external variable binding.
     *
     * @param name  variable name, with or without a leading {@code $}
     * @param value JSON value bound to the variable
     * @return new environment; {@code this} is unchanged
     */
    public EvaluationEnvironment bind(String name, JsonNode value) {
        var key = name.startsWith("$") ? name.substring(1) : name;
        var next = new HashMap<>(bindings);
        next.put(key, value);
        return new EvaluationEnvironment(next, functions, dataInspector, guardrails);
    }

    /**
     * Returns a copy with an additional host function registration.
     *
     * @param name           function name as referenced in expressions
     * @param implementation callback invoked with {@link MappingCall} at runtime
     * @return new environment; {@code this} is unchanged
     */
    public EvaluationEnvironment registerFunction(String name, Function<MappingCall, Data> implementation) {
        var fnName = name.startsWith("$") ? name : "$" + name;
        var next = new HashMap<>(functions);
        next.put(fnName, implementation);
        return new EvaluationEnvironment(bindings, next, dataInspector, guardrails);
    }

    /**
     * Returns a copy using the given {@link DataInspector} for evaluation.
     *
     * @param inspector backing-store adapter
     * @return new environment; {@code this} is unchanged
     */
    public EvaluationEnvironment withDataInspector(DataInspector inspector) {
        return new EvaluationEnvironment(bindings, functions, inspector, guardrails);
    }

    /**
     * Returns a copy with the given resource limits for evaluation.
     *
     * @param guardrails timeout, stack depth, and sequence length limits
     * @return new environment; {@code this} is unchanged
     */
    public EvaluationEnvironment withGuardrails(Guardrails guardrails) {
        return new EvaluationEnvironment(bindings, functions, dataInspector, guardrails);
    }

    /**
     * Fluent builder for environments with multiple bindings or options.
     *
     * @return new builder starting from empty bindings and functions
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Arguments supplied to a registered host function at invoke time.
     *
     * @param original           root input data for the evaluation
     * @param current            context value at the call site
     * @param arguments          unevaluated argument mappings from the expression
     * @param declaredFunctions  functions declared in enclosing blocks
     */
    public record MappingCall(Data original, Data current, java.util.List<Mapping> arguments,
                              java.util.List<DeclaredFunction> declaredFunctions) {
    }

    /**
     * Mutable accumulator for {@link EvaluationEnvironment}; produces an immutable instance on {@link #build()}.
     * <p>
     * Does not validate expression syntax or binding compatibility; that occurs at
     * {@link JSONata#jsonata(String, EvaluationEnvironment)} time.
     */
    public static final class Builder {
        private final Map<String, JsonNode> bindings = new HashMap<>();
        private final Map<String, Function<MappingCall, Data>> functions = new HashMap<>();
        private DataInspector dataInspector;
        private Guardrails guardrails = Guardrails.none();

        /**
         * Adds an external variable binding (name normalized without {@code $} prefix).
         *
         * @param name  variable name, with or without a leading {@code $}
         * @param value JSON value
         * @return this builder
         */
        public Builder bind(String name, JsonNode value) {
            var key = name.startsWith("$") ? name.substring(1) : name;
            bindings.put(key, value);
            return this;
        }

        /**
         * Registers a host function (name normalized with {@code $} prefix).
         *
         * @param name           function name as referenced in expressions
         * @param implementation callback invoked with {@link MappingCall} at runtime
         * @return this builder
         */
        public Builder registerFunction(String name, Function<MappingCall, Data> implementation) {
            var fnName = name.startsWith("$") ? name : "$" + name;
            functions.put(fnName, implementation);
            return this;
        }

        /**
         * Sets the {@link DataInspector} for evaluation; defaults to {@link DataInspectors#defaultInspector()}.
         *
         * @param inspector backing-store adapter
         * @return this builder
         */
        public Builder dataInspector(DataInspector inspector) {
            this.dataInspector = inspector;
            return this;
        }

        /**
         * Sets resource limits for evaluation; defaults to {@link Guardrails#none()}.
         *
         * @param guardrails timeout, stack depth, and sequence length limits
         * @return this builder
         */
        public Builder guardrails(Guardrails guardrails) {
            this.guardrails = guardrails;
            return this;
        }

        /**
         * Builds an immutable {@link EvaluationEnvironment} from accumulated settings.
         * Bootstraps {@link JsonFactory} when no custom inspector was set.
         *
         * @return configured environment
         */
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
