package dev.vepo.jsonata;

import java.util.List;

import dev.vepo.jsonata.functions.JSONFunctionsParser;
import dev.vepo.jsonata.functions.JSONataFunction;
import dev.vepo.jsonata.functions.data.Data;

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
 * @see JSONataFunction
 * @see JSONataResult
 */
public class JSONata {

    private final List<JSONataFunction> functions;

    private JSONata(List<JSONataFunction> expressions) {
        this.functions = expressions;
    }

    /**
     * Creates a new JSONata instance by parsing the provided JSONata expression content.
     *
     * @param content the JSONata expression content to be parsed
     *
     * @return a new JSONata instance
     */
    public static JSONata jsonata(String content) {
        return new JSONata(JSONFunctionsParser.parse(content));
    }

    public JSONataResult evaluate(String contents) {
        var data = Data.load(contents);
        return functions.stream()
                        .reduce((f1, f2) -> (o, v) -> f2.map(o, f1.map(o, v)))
                        .map(f -> f.map(data, data)
                                   .toNode())
                        .orElse(data.toNode());
    }

}