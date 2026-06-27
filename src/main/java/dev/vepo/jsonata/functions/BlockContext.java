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

/**
 * Lexical scope for block-local variables and function declarations.
 *
 * <p>Each block in a JSONata expression gets a {@code BlockContext}. Child frames
 * ({@link #createChildFrame()}) extend the scope chain so inner blocks shadow outer
 * bindings while still resolving free names from enclosing contexts.
 *
 * <p>Variables are stored as unevaluated {@link Mapping} definitions and materialized
 * on demand via {@link #variables(Data, Data)} or bound into {@link PathBindings} for
 * path-scoped references ({@code #$var}, {@code @$var}).
 *
 * @see VariableAssignment
 * @see DeclaredFunction
 */
public class BlockContext {
    private final Map<String, Mapping> variables;
    private final Map<String, DeclaredFunction> functions;
    private final List<BlockContext> parentContexts;
    private final Map<String, CompoundFunction> compoundFunctions;

    /**
     * Creates a block scope with the given parent chain (innermost parent first).
     *
     * @param parentContexts enclosing block contexts, ordered from nearest to farthest ancestor
     */
    public BlockContext(Queue<BlockContext> parentContexts) {
        this.variables = new HashMap<>();
        this.functions = new HashMap<>();
        this.compoundFunctions = new HashMap<>();
        this.parentContexts = new ArrayList<>(parentContexts);
    }

    /**
     * Creates a child frame that inherits from this context.
     *
     * @return a new scope with this context as its immediate parent
     */
    public BlockContext createChildFrame() {
        var parents = new java.util.LinkedList<>(parentContexts);
        parents.offerFirst(this);
        return new BlockContext(parents);
    }

    /**
     * Defines a block-local variable binding.
     *
     * @param identifier         the variable name (without {@code $} prefix)
     * @param variableExpression unevaluated expression; evaluated when the variable is read
     */
    public void defineVariable(String identifier, Mapping variableExpression) {
        variables.put(identifier, variableExpression);
    }

    /**
     * Defines a block-local function declaration.
     *
     * @param identifier the function name
     * @param fn         the parsed function with closure and body
     */
    public void defineFunction(String identifier, DeclaredFunction fn) {
        functions.put(identifier, fn);
    }

    /**
     * Registers a compound built-in function override for this block (e.g. {@code $sum}).
     *
     * @param fnName           the built-in name being shadowed
     * @param compoundFunction the block-scoped implementation
     */
    public void defineCompoundFunction(String fnName, CompoundFunction compoundFunction) {
        compoundFunctions.put(fnName, compoundFunction);
    }

    /**
     * Resolves a function by name, searching this frame then ancestors.
     *
     * @param identifier the function name
     * @return the declaration if found in this scope chain
     */
    public Optional<DeclaredFunction> function(String identifier) {
        return Optional.ofNullable(functions.get(identifier)).or(() -> findFunctionOnParent(identifier));
    }

    /**
     * Resolves a variable by name, searching this frame then ancestors.
     *
     * @param identifier the variable name
     * @return the unevaluated binding if found in this scope chain
     */
    public Optional<Mapping> variable(String identifier) {
        return Optional.ofNullable(variables.get(identifier)).or(() -> findVariableOnParent(identifier));
    }

    /**
     * Returns a compound built-in override defined in this frame only (not inherited).
     *
     * @param fnName the built-in name
     * @return the override if defined in this frame
     */
    public Optional<CompoundFunction> compoundFunction(String fnName) {
        return Optional.ofNullable(compoundFunctions.get(fnName));
    }

    /**
     * Materializes all in-scope variables as a single object overlay for function invocation.
     * Local bindings take precedence over parent bindings with the same name.
     *
     * @param original root input document
     * @param current  current focus value
     * @return an object whose fields are evaluated variable values
     */
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

    /**
     * Evaluates and binds all in-scope variables into {@link PathBindings} for path-scoped
     * references during nested evaluation.
     *
     * @param original root input document
     * @param current  current focus value
     */
    public void bindVariablesToPathBindings(Data original, Data current) {
        variables.forEach((key, definition) -> PathBindings.bind(key, definition.map(original, current)));
        parentContexts.forEach(context -> context.bindVariablesToPathBindings(original, current));
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
