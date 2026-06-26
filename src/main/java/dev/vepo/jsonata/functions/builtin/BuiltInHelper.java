package dev.vepo.jsonata.functions.builtin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.vepo.jsonata.functions.Mapping;
import dev.vepo.jsonata.functions.data.Data;
import dev.vepo.jsonata.functions.json.JsonFactory;

final class BuiltInHelper {

    private BuiltInHelper() {
    }

    static boolean isUndefined(Data data) {
        return data == null || data.isEmpty();
    }

    static BigDecimal toNumber(Data data) {
        if (isUndefined(data)) {
            return null;
        }
        var json = data.toJson();
        if (json.isNumber()) {
            return json.decimalValue();
        }
        if (json.isTextual()) {
            try {
                return new BigDecimal(json.asText());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (json.isBoolean()) {
            return json.asBoolean() ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        return null;
    }

    static String toString(Data data) {
        if (isUndefined(data)) {
            return "";
        }
        var json = data.toJson();
        if (json.isTextual()) {
            return json.asText();
        }
        if (json.isNull()) {
            return "";
        }
        return json.asText();
    }

    static boolean toBoolean(Data data) {
        if (isUndefined(data)) {
            return false;
        }
        var json = data.toJson();
        if (json.isBoolean()) {
            return json.asBoolean();
        }
        if (json.isNumber()) {
            return json.decimalValue().compareTo(BigDecimal.ZERO) != 0;
        }
        if (json.isTextual()) {
            return !json.asText().isEmpty();
        }
        if (json.isNull()) {
            return false;
        }
        if (data.isArray() || data.isList()) {
            return data.length() > 0;
        }
        if (data.isObject()) {
            return true;
        }
        return true;
    }

    static Data flattenArray(Data data) {
        if (data.isArray()) {
            return data;
        }
        if (data.isList()) {
            var items = new ArrayList<JsonNode>();
            for (int i = 0; i < data.length(); i++) {
                items.add(data.at(i).toJson());
            }
            return JsonFactory.json2Value(JsonFactory.arrayNode(items));
        }
        return data;
    }

    static List<Data> distinctValues(Data data) {
        var seen = new LinkedHashSet<String>();
        var results = new ArrayList<Data>();
        if (data.isArray() || data.isList()) {
            for (int i = 0; i < data.length(); i++) {
                var element = data.at(i);
                var key = element.toJson() == null ? "null" : element.toJson().toString();
                if (seen.add(key)) {
                    results.add(element);
                }
            }
        } else if (!isUndefined(data)) {
            results.add(data);
        }
        return results;
    }

    static Mapping contextExtractor(List<Mapping> providers) {
        if (providers.isEmpty()) {
            return (o, c) -> c;
        }
        return providers.get(0);
    }
}
