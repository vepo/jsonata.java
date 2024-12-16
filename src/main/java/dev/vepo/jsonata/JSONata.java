package dev.vepo.jsonata;

import java.util.List;

import dev.vepo.jsonata.expression.Expression;
import dev.vepo.jsonata.expression.Expressions;
import dev.vepo.jsonata.expression.Node;
import dev.vepo.jsonata.expression.transformers.JsonValue;

/**
 * The JSONata class provides functionality to parse and evaluate JSONata expressions.
 * JSONata is a lightweight query and transformation language for JSON data.
 * 
 * <p>This class allows creating a JSONata instance from a list of expressions or by parsing
 * a JSONata expression content string. It also provides a method to evaluate JSON content
 * against the parsed expressions.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * JSONata jsonata = JSONata.jsonata("expression content");
 * Node result = jsonata.evaluate("json content");
 * }</pre>
 * 
 * <p>Methods:</p>
 * <ul>
 *   <li>{@link #JSONata(List)} - Constructs a JSONata instance with a list of expressions.</li>
 *   <li>{@link #jsonata(String)} - Parses the provided JSONata expression content and returns a new JSONata instance.</li>
 *   <li>{@link #evaluate(String)} - Evaluates the provided JSON content against the parsed expressions and returns the result as a Node.</li>
 * </ul>
 * 
 * @see Expression
 * @see Node
 * @see JsonValue
 */
public class JSONata {

    private List<Expression> expressions;

    public JSONata(List<Expression> expressions) {
        this.expressions = expressions;
    }

    /**
     * Creates a new JSONata instance by parsing the provided JSONata expression content.
     *
     * @param content the JSONata expression content to be parsed
     * @return a new JSONata instance
     */
    public static JSONata jsonata(String content) {
        return new JSONata(Expressions.parse(content));
    }

    public Node evaluate(String content) {
        return new JsonValue(content).apply(expressions);
    }

}