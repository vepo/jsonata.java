package dev.vepo.jsonata;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.jsonata.expression.JSONataExpression;

public class JSONata {

    private List<Expression> expressions;

    public JSONata(List<Expression> expressions) {
        this.expressions = expressions;
    }

    @FunctionalInterface
    public interface Expression {
        Node map(Node node);
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    public static JSONata of(String content) {
        return JSONataExpression.parse(content);
    }

    public Node evaluate(String content) {
        try {
            Node currNode = new NodeObject(mapper.readTree(content));
            for (var exp : expressions) {
                currNode = exp.map(currNode);
            }
            return currNode;
        } catch (JsonProcessingException e) {
            throw new RuntimeException();
        }
    }

}