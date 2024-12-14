package dev.vepo.jsonata;

import java.util.List;

import dev.vepo.jsonata.expression.Expression;
import dev.vepo.jsonata.expression.JSONataExpression;
import dev.vepo.jsonata.expression.JsonValue;

public class JSONata {

    private List<Expression> expressions;

    public JSONata(List<Expression> expressions) {
        this.expressions = expressions;
    }


    public static JSONata of(String content) {
        return JSONataExpression.parse(content);
    }

    public Node evaluate(String content) {
        return new JsonValue(content).apply(expressions);
    }

}