package dev.vepo.jsonata;

import java.util.List;

import dev.vepo.jsonata.expression.Expression;
import dev.vepo.jsonata.expression.Expressions;
import dev.vepo.jsonata.expression.transformers.JsonValue;

public class JSONata {

    private List<Expression> expressions;

    public JSONata(List<Expression> expressions) {
        this.expressions = expressions;
    }


    public static JSONata of(String content) {
        return Expressions.parse(content);
    }

    public Node evaluate(String content) {
        return new JsonValue(content).apply(expressions);
    }

}