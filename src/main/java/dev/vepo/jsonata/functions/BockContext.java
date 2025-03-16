package dev.vepo.jsonata.functions;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BockContext {
    private final Map<String, JSONataFunction> variables;
    private final Map<String, DeclaredFunction> functions;

    public BockContext() {
        this.variables = new HashMap<>();
        this.functions = new HashMap<>();
    }

    public void defineVariable(String identifier, JSONataFunction variableExpression) {
        variables.put(identifier, variableExpression);
    }

    public void defineFunction(String identifier, DeclaredFunction fn) {
        functions.put(identifier, fn);
    }

    public Optional<DeclaredFunction> function(String identifier) {
        return Optional.ofNullable(functions.get(identifier));
    }

    public Optional<JSONataFunction> variable(String identifier) {
        return Optional.ofNullable(variables.get(identifier));
    }

}
