package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Runtime variable assignment inside a block ({@code $x := expr}).
 *
 * <p>Registers the binding in {@link BlockContext} and returns empty. Function assignments
 * capture the current focus as closure context; expression assignments are evaluated once
 * and stored as constant bindings via {@link VariableCell}.
 *
 * @param block the block scope receiving the binding
 * @param name  variable name (without {@code $} prefix)
 * @param rhs   unevaluated expression, {@link DeclaredFunction}, or {@link Mapping}
 */
public record VariableAssignment(BlockContext block, String name, Object rhs) implements Mapping {

    /**
     * Assigns the result of evaluating {@code rhs} as a mapping expression.
     *
     * @param block the block scope
     * @param name  variable name
     * @param rhs   the right-hand-side expression
     */
    public VariableAssignment(BlockContext block, String name, Mapping rhs) {
        this(block, name, (Object) rhs);
    }

    /**
     * Assigns a function declaration, capturing current focus as closure context.
     *
     * @param block the block scope
     * @param name  variable name
     * @param rhs   the declared function
     */
    public VariableAssignment(BlockContext block, String name, DeclaredFunction rhs) {
        this(block, name, (Object) rhs);
    }

    /** {@inheritDoc} */
    @Override
    public Data map(Data original, Data current) {
        var cell = new VariableCell();
        block.defineVariable(name, cell);
        Mapping binding;
        if (rhs instanceof DeclaredFunction fn) {
            final Data capturedContext = current;
            binding = (o, c) -> FunctionValues.wrap(fn.asValue().withCapturedContext(capturedContext));
        } else {
            var expr = (Mapping) rhs;
            final Data value = expr.map(original, current);
            binding = (o, c) -> value;
        }
        cell.set(binding);
        return Mapping.empty();
    }
}
