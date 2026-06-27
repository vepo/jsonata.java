package dev.vepo.jsonata.functions.signature;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import dev.vepo.jsonata.exception.JSONataException;
import dev.vepo.jsonata.functions.FunctionValues;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.data.FunctionData;

/**
 * Parses and validates JSONata function signatures such as {@code <a<s>s?:s>}.
 */
public final class FunctionSignature {

    private final String definition;
    private final List<ParamSpec> params;
    private final Pattern regex;

    private FunctionSignature(String definition, List<ParamSpec> params, Pattern regex) {
        this.definition = definition;
        this.params = params;
        this.regex = regex;
    }

    public String definition() {
        return definition;
    }

    public static FunctionSignature parse(String signature) {
        var params = new ArrayList<ParamSpec>();
        ParamSpec param = new ParamSpec();
        ParamSpec prevParam = param;
        int position = 1;
        while (position < signature.length()) {
            char symbol = signature.charAt(position);
            if (symbol == ':') {
                break;
            }
            switch (symbol) {
                case 's', 'n', 'b', 'l', 'o' -> {
                    param.regex = "[" + symbol + "m]";
                    param.type = String.valueOf(symbol);
                    params.add(param);
                    prevParam = param;
                    param = new ParamSpec();
                }
                case 'a' -> {
                    param.regex = "[asnblfom]";
                    param.type = "a";
                    param.array = true;
                    params.add(param);
                    prevParam = param;
                    param = new ParamSpec();
                }
                case 'f' -> {
                    param.regex = "f";
                    param.type = "f";
                    params.add(param);
                    prevParam = param;
                    param = new ParamSpec();
                }
                case 'j' -> {
                    param.regex = "[asnblom]";
                    param.type = "j";
                    params.add(param);
                    prevParam = param;
                    param = new ParamSpec();
                }
                case 'x' -> {
                    param.regex = "[asnblfom]";
                    param.type = "x";
                    params.add(param);
                    prevParam = param;
                    param = new ParamSpec();
                }
                case '-' -> {
                    prevParam.context = true;
                    prevParam.contextPattern = Pattern.compile(prevParam.regex);
                    prevParam.regex = prevParam.regex + "?";
                }
                case '?', '+' -> prevParam.regex = prevParam.regex + symbol;
                case '(' -> {
                    int endParen = findClosingBracket(signature, position, '(', ')');
                    var choice = signature.substring(position + 1, endParen);
                    if (choice.contains("<")) {
                        throw new JSONataException("S0402", "Parameterized choice groups not supported: " + choice);
                    }
                    param.regex = "[" + choice + "m]";
                    param.type = "(" + choice + ")";
                    position = endParen;
                    params.add(param);
                    prevParam = param;
                    param = new ParamSpec();
                }
                case '<' -> {
                    if (!"a".equals(prevParam.type) && !"f".equals(prevParam.type)) {
                        throw new JSONataException("S0401", "Type parameter on non-array/function: " + prevParam.type);
                    }
                    int endPos = findClosingBracket(signature, position, '<', '>');
                    prevParam.subtype = signature.substring(position + 1, endPos);
                    position = endPos;
                }
                default -> {
                }
            }
            position++;
        }
        var regexStr = "^" + params.stream().map(p -> "(" + p.regex + ")").reduce("", String::concat) + "$";
        return new FunctionSignature(signature, params, Pattern.compile(regexStr));
    }

    public List<Data> validate(List<Data> args, Data context) {
        var suppliedSig = new StringBuilder();
        for (var arg : args) {
            suppliedSig.append(typeSymbol(arg));
        }
        var matcher = regex.matcher(suppliedSig.toString());
        if (!matcher.matches()) {
            throw new JSONataException("T0410", "Argument signature mismatch at index 1");
        }
        var validated = new ArrayList<Data>();
        int argIndex = 0;
        for (int i = 0; i < params.size(); i++) {
            var spec = params.get(i);
            var match = matcher.group(i + 1);
            if (match.isEmpty()) {
                if (spec.context && spec.contextPattern != null) {
                    var contextType = typeSymbol(context);
                    if (!spec.contextPattern.matcher(contextType).matches()) {
                        throw new JSONataException("T0411", "Context value wrong type for parameter " + (argIndex + 1));
                    }
                    validated.add(context);
                } else {
                    validated.add(args.get(argIndex));
                    argIndex++;
                }
            } else {
                for (char single : match.toCharArray()) {
                    var arg = args.get(argIndex);
                    if ("a".equals(spec.type)) {
                        if (single == 'm') {
                            validated.add(dev.vepo.jsonata.functions.Mapping.empty());
                        } else {
                            if (single != 'a' && spec.subtype != null && !match.equals(spec.subtype)) {
                                // wrap scalar in array
                                validated.add(wrapAsArray(arg));
                            } else if (single == 'a' && spec.subtype != null && arg.isArray()) {
                                validateArraySubtype(arg, spec.subtype, argIndex);
                                validated.add(arg);
                            } else if (single != 'a') {
                                validated.add(wrapAsArray(arg));
                            } else {
                                validated.add(arg);
                            }
                            argIndex++;
                        }
                    } else {
                        validated.add(arg);
                        argIndex++;
                    }
                }
            }
        }
        return validated;
    }

    private static void validateArraySubtype(Data arg, String subtype, int argIndex) {
        for (int i = 0; i < arg.length(); i++) {
            if (!subtype.startsWith(String.valueOf(typeSymbol(arg.at(i)).charAt(0)))) {
                throw new JSONataException("T0412", "Array element type mismatch at index " + (argIndex + 1));
            }
        }
    }

    private static Data wrapAsArray(Data arg) {
        return new dev.vepo.jsonata.functions.data.GroupedData(java.util.List.of(arg));
    }

    private static String typeSymbol(Data value) {
        if (FunctionValues.isFunction(value)) {
            return "f";
        }
        if (value == null || value.isEmpty()) {
            return "m";
        }
        var json = value.toJson();
        if (json == null) {
            return "m";
        }
        if (json.isTextual()) {
            return "s";
        }
        if (json.isNumber()) {
            return "n";
        }
        if (json.isBoolean()) {
            return "b";
        }
        if (json.isNull()) {
            return "l";
        }
        if (value.isArray() || value.isList()) {
            return "a";
        }
        if (value.isObject()) {
            return "o";
        }
        return "m";
    }

    private static int findClosingBracket(String str, int start, char open, char close) {
        int depth = 1;
        int position = start;
        while (position < str.length()) {
            position++;
            char symbol = str.charAt(position);
            if (symbol == close) {
                depth--;
                if (depth == 0) {
                    break;
                }
            } else if (symbol == open) {
                depth++;
            }
        }
        return position;
    }

    private static final class ParamSpec {
        String regex = "";
        String type = "";
        boolean array;
        boolean context;
        Pattern contextPattern;
        String subtype;
    }
}
