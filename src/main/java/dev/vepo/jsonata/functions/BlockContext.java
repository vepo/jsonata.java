package dev.vepo.jsonata.functions;

import static dev.vepo.jsonata.functions.json.JsonFactory.objectBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Predicate;

import dev.vepo.jsonata.functions.builtin.CompoundFunction;
import dev.vepo.jsonata.functions.data.Data;

public class BlockContext {
    private final Map<String, Mapping> variables;
    private final Map<String, DeclaredFunction> functions;
    private final List<BlockContext> parentContexts;
    private final Map<String, CompoundFunction> compoundFunctions;

    public BlockContext(Queue<BlockContext> parentContexts) {
        this.variables = new HashMap<>();
        this.functions = new HashMap<>();
        this.compoundFunctions = new HashMap<>();
        this.parentContexts = new ArrayList<>(parentContexts);
    }

    public void defineVariable(String identifier, Mapping variableExpression) {
        variables.put(identifier, variableExpression);
    }

    public void defineFunction(String identifier, DeclaredFunction fn) {
        functions.put(identifier, fn);
    }

    public void defineCompoundFunction(String fnName, CompoundFunction compoundFunction) {
        compoundFunctions.put(fnName, compoundFunction);
    }

    public Optional<DeclaredFunction> function(String identifier) {
        return Optional.ofNullable(functions.get(identifier)).or(() -> findFunctionOnParent(identifier));
    }

    public Optional<Mapping> variable(String identifier) {
        return Optional.ofNullable(variables.get(identifier)).or(() -> findVariableOnParent(identifier));
    }

    public Optional<CompoundFunction> compoundFunction(String fnName) {
        return Optional.ofNullable(compoundFunctions.get(fnName));
    }

    public Data variables(Data original, Data current) {
        var builder = objectBuilder();
        variables.forEach((key, definition) -> builder.set(key, definition.map(original, current)));
        parentContexts.forEach(context -> context.variables.forEach((key, definition) -> {
            if (!builder.hasValue(key)) {
                builder.set(key, definition.map(original, current));
            }
        }));
        return builder.build();
    }

    private Optional<DeclaredFunction> findFunctionOnParent(String identifier) {
        return parentContexts.stream()
                             .map(c -> c.function(identifier))
                             .filter(Predicate.not(Optional::isEmpty))
                             .map(Optional::get)
                             .findFirst();
    }

    private Optional<Mapping> findVariableOnParent(String identifier) {
        return parentContexts.stream()
                             .map(c -> c.variable(identifier))
                             .filter(Predicate.not(Optional::isEmpty))
                             .map(Optional::get)
                             .findFirst();
    }

}
