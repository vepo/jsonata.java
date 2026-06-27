package dev.vepo.jsonata;

import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.EvaluationContext;
import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.MappingParser;
import dev.vepo.jsonata.functions.PathBindings;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.DataInspector;
import dev.vepo.jsonata.functions.json.JsonFactory;

/**
 * Application entry point for compile-once, evaluate-many JSONata usage.
 * <p>
 * Layer: <strong>application facade</strong>. Holds a compiled {@link Mapping} pipeline and an
 * {@link EvaluationEnvironment}; parsing stays in {@code parser} / {@link MappingParser} and is
 * not repeated on each {@link #evaluateData(Data)} call.
 * <p>
 * Usage pattern:
 * <pre>{@code
 * var j = JSONata.jsonata("$sum(items.price)", env);
 * var result = j.evaluate(jsonInput);
 * }</pre>
 * Bindings and functions that affect compile-time resolution must be supplied via
 * {@link #jsonata(String, EvaluationEnvironment)}. {@link #bind(String, JsonNode)} and
 * {@link #registerFunction(String, Function)} return new instances with merged environment but
 * reuse the existing compiled mappings.
 * <p>
 * Invariants: instances are immutable; evaluation runs under the environment's
 * {@link DataInspector} and {@link Guardrails} via {@link EvaluationContext}.
 */
public class JSONata {

    private final List<Mapping> mappings;
    private final Mapping composedMapping;
    private final EvaluationEnvironment environment;

    private JSONata(List<Mapping> mappings, EvaluationEnvironment environment) {
        this.mappings = mappings;
        this.composedMapping = compose(mappings);
        this.environment = environment;
    }

    private static Mapping compose(List<Mapping> mappings) {
        return mappings.stream()
                       .reduce((f1, f2) -> (o, v) -> f2.map(o, f1.map(o, v)))
                       .orElse(null);
    }

    /**
     * Parses {@code content} with an empty {@link EvaluationEnvironment}.
     *
     * @param content JSONata expression text
     * @return a compiled instance ready for evaluation
     * @throws dev.vepo.jsonata.exception.JSONataException if parsing fails
     */
    public static JSONata jsonata(String content) {
        return jsonata(content, EvaluationEnvironment.empty());
    }

    /**
     * Parses {@code content} using the given {@link DataInspector} for compile-time data access.
     *
     * @param content   JSONata expression text
     * @param inspector backing-store adapter applied during evaluation
     * @return a compiled instance ready for evaluation
     * @throws dev.vepo.jsonata.exception.JSONataException if parsing fails
     */
    public static JSONata jsonata(String content, DataInspector inspector) {
        return jsonata(content, EvaluationEnvironment.empty().withDataInspector(inspector));
    }

    /**
     * Parses {@code content} with the supplied environment (bindings, functions, inspector, guardrails).
     *
     * @param content     JSONata expression text
     * @param environment compile-time bindings and registered functions
     * @return a compiled instance ready for evaluation
     * @throws dev.vepo.jsonata.exception.JSONataException if parsing fails
     */
    public static JSONata jsonata(String content, EvaluationEnvironment environment) {
        return new JSONata(MappingParser.parse(content, environment), environment);
    }

    /**
     * Parses {@code contents} as JSON input and evaluates the compiled expression against it.
     *
     * @param contents JSON document as a string
     * @return caller-facing evaluation result
     * @throws dev.vepo.jsonata.exception.JSONataException on parse or evaluation failure
     */
    public JSONataResult evaluate(String contents) {
        var data = EvaluationContext.call(environment.dataInspector(), environment.guardrails(),
                () -> JsonFactory.fromString(contents));
        return evaluateData(data);
    }

    /**
     * Evaluates the compiled expression against {@code data}.
     * <p>
     * Clears {@link PathBindings} around the mapping call so path state does not leak between
     * evaluations. An empty expression returns {@code data} unchanged as a {@link JSONataResult}.
     *
     * @param data input context and backing values for the mapping pipeline
     * @return caller-facing evaluation result
     * @throws dev.vepo.jsonata.exception.JSONataException on evaluation failure or guardrail breach
     */
    public JSONataResult evaluateData(Data data) {
        return EvaluationContext.call(environment.dataInspector(), environment.guardrails(), () -> {
            PathBindings.clearBindings();
            PathBindings.clearParents();
            try {
                if (composedMapping == null) {
                    return data.toNode();
                }
                return composedMapping.map(data, data).toNode();
            } finally {
                PathBindings.clearBindings();
                PathBindings.clearParents();
            }
        });
    }

    /**
     * Returns a new instance with an external variable binding merged into the environment.
     * Does not re-parse the expression.
     *
     * @param name  variable name, with or without a leading {@code $}
     * @param value JSON value bound to the variable
     * @return new instance sharing compiled mappings
     */
    public JSONata bind(String name, JsonNode value) {
        return new JSONata(mappings, environment.bind(name, value));
    }

    /**
     * Returns a new instance with an external variable binding merged into the environment.
     * Does not re-parse the expression.
     *
     * @param name      variable name, with or without a leading {@code $}
     * @param jsonValue JSON document as a string
     * @return new instance sharing compiled mappings
     * @throws dev.vepo.jsonata.exception.JSONataException if {@code jsonValue} is not valid JSON
     */
    public JSONata bind(String name, String jsonValue) {
        return bind(name, JsonFactory.fromString(jsonValue).toJson());
    }

    /**
     * Returns a new instance with a host function registered for use in expressions.
     * Does not re-parse the expression.
     *
     * @param name           function name as referenced in the expression
     * @param implementation callback receiving {@link EvaluationEnvironment.MappingCall} at invoke time
     * @return new instance sharing compiled mappings
     */
    public JSONata registerFunction(String name, Function<EvaluationEnvironment.MappingCall, Data> implementation) {
        return new JSONata(mappings, environment.registerFunction(name, implementation));
    }

    /**
     * Environment used for parsing and evaluation (bindings, functions, inspector, guardrails).
     *
     * @return the active evaluation environment
     */
    public EvaluationEnvironment environment() {
        return environment;
    }

    List<Mapping> mappings() {
        return mappings;
    }
}
