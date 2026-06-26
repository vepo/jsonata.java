package dev.vepo.jsonata.functions.regex;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

public class RegExp {

    public record MatchResult(String match, int index, List<String> groups) {
    }

    private static ScriptEngine loadEngine() {
        ScriptEngineManager engineManager = new ScriptEngineManager();
        return engineManager.getEngineByName("nashorn");
    }

    private static void invalidPattern(String pattern, ScriptException e) {
        throw new IllegalArgumentException("Invalid pattern: " + pattern, e);
    }

    private final Function<String, Boolean> isContainedFn;
    private final Function<String, String[]> splitFn;
    private final Function<String, MatchResult> matchFn;
    private final Function<String, List<MatchResult>> matchAllFn;
    private final ScriptEngine engine;
    private final String pattern;

    public RegExp(String pattern) {
        this.pattern = pattern;
        engine = loadEngine();
        try {
            engine.eval(String.format("""
                                      var pattern = %s;
                                      function isContained(content) {
                                          return content.match(pattern) !== null;
                                      }
                                      function split(content) {
                                          return content.split(pattern);
                                      }
                                      function firstMatch(content) {
                                          var m = content.match(pattern);
                                          if (m === null) return null;
                                          var groups = [];
                                          for (var i = 1; i < m.length; i++) {
                                              groups.push(m[i] !== undefined ? m[i] : '');
                                          }
                                          return { match: m[0], index: m.index, groups: groups };
                                      }
                                      function allMatches(content) {
                                          var results = [];
                                          var flags = pattern.flags || '';
                                          var globalPattern = flags.indexOf('g') >= 0 ? pattern
                                              : new RegExp(pattern.source, flags + 'g');
                                          var m;
                                          while ((m = globalPattern.exec(content)) !== null) {
                                              var groups = [];
                                              for (var i = 1; i < m.length; i++) {
                                                  groups.push(m[i] !== undefined ? m[i] : '');
                                              }
                                              results.push({ match: m[0], index: m.index, groups: groups });
                                              if (m[0].length === 0) globalPattern.lastIndex++;
                                          }
                                          return results;
                                      }
                                      function replaceString(content, replacement, limit) {
                                          if (limit === 0) return content;
                                          if (limit < 0) return content.replace(pattern, replacement);
                                          var count = 0;
                                          return content.replace(pattern, function() {
                                              if (limit >= 0 && count >= limit) return arguments[0];
                                              count++;
                                              var args = Array.prototype.slice.call(arguments);
                                              var groups = args.slice(1, args.length - 2);
                                              var result = replacement;
                                              result = result.replace(/\\$(\\d+)/g, function(_, n) {
                                                  var idx = parseInt(n, 10);
                                                  return idx === 0 ? args[0] : (groups[idx - 1] || '');
                                              });
                                              result = result.replace(/\\$&/g, args[0]);
                                              return result;
                                          });
                                      }
                                      """, pattern));
            isContainedFn = content -> evalBoolean("isContained(content)", content);
            splitFn = content -> evalStringArray("split(content)", content);
            matchFn = content -> evalMatch("firstMatch(content)", content);
            matchAllFn = content -> evalMatchList("allMatches(content)", content);
        } catch (ScriptException e) {
            throw new IllegalArgumentException("Invalid pattern: " + pattern, e);
        }
    }

    public Boolean isContainedIn(String content) {
        return isContainedFn.apply(content);
    }

    public String[] split(String value) {
        return splitFn.apply(value);
    }

    public MatchResult match(String content) {
        return matchFn.apply(content);
    }

    public List<MatchResult> matchAll(String content) {
        return matchAllFn.apply(content);
    }

    public String replace(String content, String replacement, int limit) {
        try {
            var bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.put("content", content);
            bindings.put("replacement", replacement);
            bindings.put("limit", limit);
            return (String) engine.eval("replaceString(content, replacement, limit)", bindings);
        } catch (ScriptException e) {
            invalidPattern(pattern, e);
            return content;
        }
    }

    public String replace(String content, Function<MatchResult, String> callback, int limit) {
        var results = matchAll(content);
        if (results.isEmpty()) {
            return content;
        }
        var sb = new StringBuilder();
        int lastEnd = 0;
        int count = 0;
        for (var result : results) {
            if (limit >= 0 && count >= limit) {
                break;
            }
            sb.append(content, lastEnd, result.index());
            sb.append(callback.apply(result));
            lastEnd = result.index() + result.match().length();
            count++;
        }
        sb.append(content.substring(lastEnd));
        return sb.toString();
    }

    private Boolean evalBoolean(String script, String content) {
        try {
            var bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.put("content", content);
            return (Boolean) engine.eval(script, bindings);
        } catch (ScriptException e) {
            invalidPattern(pattern, e);
            return false;
        }
    }

    private String[] evalStringArray(String script, String content) {
        try {
            var bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.put("content", content);
            ScriptObjectMirror ret = (ScriptObjectMirror) engine.eval(script, bindings);
            return ret.to(String[].class);
        } catch (ScriptException e) {
            invalidPattern(pattern, e);
            return new String[] { content };
        }
    }

    private MatchResult evalMatch(String script, String content) {
        try {
            var bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.put("content", content);
            var ret = engine.eval(script, bindings);
            if (ret == null) {
                return null;
            }
            ScriptObjectMirror mirror = (ScriptObjectMirror) ret;
            var groups = new ArrayList<String>();
            ScriptObjectMirror groupsMirror = (ScriptObjectMirror) mirror.get("groups");
            if (groupsMirror != null) {
                groupsMirror.values().forEach(v -> groups.add(String.valueOf(v)));
            }
            return new MatchResult(String.valueOf(mirror.get("match")),
                    ((Number) mirror.get("index")).intValue(), groups);
        } catch (ScriptException e) {
            invalidPattern(pattern, e);
            return null;
        }
    }

    private List<MatchResult> evalMatchList(String script, String content) {
        try {
            var bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.put("content", content);
            ScriptObjectMirror ret = (ScriptObjectMirror) engine.eval(script, bindings);
            var results = new ArrayList<MatchResult>();
            ret.values().forEach(v -> {
                ScriptObjectMirror mirror = (ScriptObjectMirror) v;
                var groups = new ArrayList<String>();
                ScriptObjectMirror groupsMirror = (ScriptObjectMirror) mirror.get("groups");
                if (groupsMirror != null) {
                    groupsMirror.values().forEach(g -> groups.add(String.valueOf(g)));
                }
                results.add(new MatchResult(String.valueOf(mirror.get("match")),
                        ((Number) mirror.get("index")).intValue(), groups));
            });
            return results;
        } catch (ScriptException e) {
            invalidPattern(pattern, e);
            return List.of();
        }
    }
}
