package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Runtime variable assignment inside a block ({@code $x := expr}).
 */
public record VariableAssignment(BlockContext block, String name, Object rhs) implements Mapping {

    public VariableAssignment(BlockContext block, String name, Mapping rhs) {
        this(block, name, (Object) rhs);
    }

    public VariableAssignment(BlockContext block, String name, DeclaredFunction rhs) {
        this(block, name, (Object) rhs);
    }

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
