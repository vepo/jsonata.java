package dev.vepo.jsonata.conformance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.jsonata.EvaluationEnvironment;
import dev.vepo.jsonata.Guardrails;
import dev.vepo.jsonata.JSONata;
import dev.vepo.jsonata.exception.JSONataException;

public record ConformanceCase(String group, String caseName, String expr, JsonNode data,
                              JsonNode bindings, JsonNode expectedResult, boolean undefinedResult,
                              String expectedCode, Long timelimitMs, Integer maxDepth) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Path suiteRoot() {
        return Path.of("src/test/resources/jsonata-js/test/test-suite");
    }

    public static List<ConformanceCase> loadAll() throws IOException {
        var root = suiteRoot();
        var datasets = loadDatasets(root.resolve("datasets"));
        var cases = new ArrayList<ConformanceCase>();
        try (var groups = Files.list(root.resolve("groups"))) {
            groups.filter(Files::isDirectory)
                  .forEach(groupDir -> loadGroup(groupDir, datasets, cases));
        }
        cases.sort(Comparator.comparing(ConformanceCase::group).thenComparing(ConformanceCase::caseName));
        return cases;
    }

    public static Map<String, ConformanceReport> runAll(List<ConformanceCase> cases) {
        var byGroup = new LinkedHashMap<String, ConformanceReport>();
        for (var c : cases) {
            if (ConformanceSkipList.isSkipped(c.group(), c.caseName())) {
                continue;
            }
            var report = byGroup.computeIfAbsent(c.group(), ConformanceReport::new);
            report.total++;
            try {
                if (c.run()) {
                    report.passed++;
                } else {
                    report.failed++;
                    report.failures.add(c.caseName());
                }
            } catch (Exception e) {
                report.failed++;
                report.failures.add(c.caseName() + ": " + e.getMessage());
            }
        }
        return byGroup;
    }

    public boolean run() {
        try {
            var env = buildEnvironment();
            var jsonata = JSONata.jsonata(expr, env);
            if (expectedCode != null) {
                try {
                    jsonata.evaluateData(data != null ? dev.vepo.jsonata.functions.data.Data.load(data.toString()) :
                                              dev.vepo.jsonata.functions.data.Data.load("{}"));
                    return matchesExpectedCode(null);
                } catch (JSONataException e) {
                    return matchesExpectedCode(e.code().orElse(null));
                } catch (IllegalArgumentException | org.antlr.v4.runtime.misc.ParseCancellationException e) {
                    return matchesExpectedCode(null);
                }
            }
            var input = data != null ? data.toString() : "{}";
            var result = jsonata.evaluate(input);
            if (undefinedResult) {
                return result.isEmpty();
            }
            if (expectedResult == null) {
                return result.isEmpty();
            }
            return JsonComparison.equivalent(expectedResult, JsonComparison.resultToJson(result));
        } catch (org.antlr.v4.runtime.misc.ParseCancellationException e) {
            return expectedCode != null;
        }
    }

    private boolean matchesExpectedCode(String actual) {
        if (expectedCode == null) {
            return true;
        }
        return expectedCode.equals(actual);
    }

    private EvaluationEnvironment buildEnvironment() {
        var builder = EvaluationEnvironment.builder();
        if (bindings != null && bindings.isObject()) {
            bindings.fields().forEachRemaining(e -> builder.bind(e.getKey(), e.getValue()));
        }
        if (timelimitMs != null || maxDepth != null) {
            var guardrails = new Guardrails(
                    timelimitMs != null ? java.util.OptionalLong.of(timelimitMs) : java.util.OptionalLong.empty(),
                    maxDepth != null ? java.util.OptionalInt.of(maxDepth) : java.util.OptionalInt.empty(),
                    java.util.OptionalInt.empty());
            builder.guardrails(guardrails);
        }
        return builder.build();
    }

    private static void loadGroup(Path groupDir, Map<String, JsonNode> datasets, List<ConformanceCase> cases) {
        var group = groupDir.getFileName().toString();
        try (Stream<Path> files = Files.list(groupDir)) {
            files.filter(p -> p.getFileName().toString().startsWith("case") && p.toString().endsWith(".json"))
                 .forEach(file -> {
                     try {
                         parseCases(group, file, datasets, cases);
                     } catch (IOException e) {
                         throw new RuntimeException(e);
                     }
                 });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void parseCases(String group, Path file, Map<String, JsonNode> datasets,
                                   List<ConformanceCase> cases) throws IOException {
        var node = MAPPER.readTree(file.toFile());
        if (node.isArray()) {
            var baseName = file.getFileName().toString().replace(".json", "");
            for (int i = 0; i < node.size(); i++) {
                var c = parseCase(group, baseName + "_" + i, node.get(i), datasets);
                if (c != null) {
                    cases.add(c);
                }
            }
        } else {
            var c = parseCase(group, file.getFileName().toString().replace(".json", ""), node, datasets);
            if (c != null) {
                cases.add(c);
            }
        }
    }

    private static ConformanceCase parseCase(String group, String caseName, JsonNode node,
                                             Map<String, JsonNode> datasets) throws IOException {
        if (!node.has("expr")) {
            return null;
        }
        JsonNode data;
        if (node.has("data")) {
            data = node.get("data");
        } else if (node.has("dataset") && !node.get("dataset").isNull()) {
            data = datasets.get(node.get("dataset").asText());
        } else {
            data = null;
        }
        return new ConformanceCase(
                group,
                caseName,
                node.get("expr").asText(),
                data,
                node.has("bindings") ? node.get("bindings") : null,
                node.has("result") ? node.get("result") : null,
                node.has("undefinedResult") && node.get("undefinedResult").asBoolean(),
                node.has("code") ? node.get("code").asText() : null,
                node.has("timelimit") ? node.get("timelimit").asLong() : null,
                node.has("depth") ? node.get("depth").asInt() : null);
    }

    private static Map<String, JsonNode> loadDatasets(Path datasetsDir) throws IOException {
        var map = new LinkedHashMap<String, JsonNode>();
        if (!Files.isDirectory(datasetsDir)) {
            return map;
        }
        try (var files = Files.list(datasetsDir)) {
            files.filter(p -> p.toString().endsWith(".json"))
                 .forEach(p -> {
                     try {
                         map.put(p.getFileName().toString().replace(".json", ""),
                                 MAPPER.readTree(p.toFile()));
                     } catch (IOException e) {
                         throw new RuntimeException(e);
                     }
                 });
        }
        return map;
    }

    public static final class ConformanceReport {
        final String group;
        int total;
        int passed;
        int failed;
        final List<String> failures = new ArrayList<>();

        ConformanceReport(String group) {
            this.group = group;
        }

        double passRate() {
            return total == 0 ? 0 : (100.0 * passed / total);
        }
    }
}
