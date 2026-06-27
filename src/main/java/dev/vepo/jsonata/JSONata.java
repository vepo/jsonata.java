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
 * Facade for parsing and evaluating JSONata expressions.
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

    public static JSONata jsonata(String content) {
        return jsonata(content, EvaluationEnvironment.empty());
    }

    public static JSONata jsonata(String content, DataInspector inspector) {
        return jsonata(content, EvaluationEnvironment.empty().withDataInspector(inspector));
    }

    public static JSONata jsonata(String content, EvaluationEnvironment environment) {
        return new JSONata(MappingParser.parse(content, environment), environment);
    }

    public JSONataResult evaluate(String contents) {
        var data = EvaluationContext.call(environment.dataInspector(), environment.guardrails(),
                () -> JsonFactory.fromString(contents));
        return evaluateData(data);
    }

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

    public JSONata bind(String name, JsonNode value) {
        return new JSONata(mappings, environment.bind(name, value));
    }

    public JSONata bind(String name, String jsonValue) {
        return bind(name, JsonFactory.fromString(jsonValue).toJson());
    }

    public JSONata registerFunction(String name, Function<EvaluationEnvironment.MappingCall, Data> implementation) {
        return new JSONata(mappings, environment.registerFunction(name, implementation));
    }

    public EvaluationEnvironment environment() {
        return environment;
    }

    List<Mapping> mappings() {
        return mappings;
    }
}
