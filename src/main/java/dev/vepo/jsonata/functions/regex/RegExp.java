package dev.vepo.jsonata.functions.regex;

import java.util.function.Function;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class RegExp {

    private static ScriptEngine loadEngine() {
        ScriptEngineManager engineManager = new ScriptEngineManager();
        return engineManager.getEngineByName("nashorn");
    }

    private final Function<String, Boolean> isContainedFn;
    private final ScriptEngine engine;

    public RegExp(String pattern) {
        engine = loadEngine();
        try {
            engine.eval(String.format("""
                                      var pattern = %s;
                                      function isContained(content) {
                                          return content.match(pattern) !== null;
                                      }
                                      """, pattern));
            isContainedFn = content -> {
                try {
                    var bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
                    bindings.put("content", content);
                    return (Boolean) engine.eval("isContained(content)", bindings);
                } catch (ScriptException e) {
                    throw new IllegalArgumentException("Invalid pattern: " + pattern, e);
                }
            };
        } catch (ScriptException e) {
            throw new IllegalArgumentException("Invalid pattern: " + pattern, e);
        }
    }

    public Boolean isContainedIn(String content) {
        return isContainedFn.apply(content);
    }
}
