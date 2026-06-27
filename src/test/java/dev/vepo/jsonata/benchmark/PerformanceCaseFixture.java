package dev.vepo.jsonata.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;

public record PerformanceCaseFixture(String name, String expr, String inputJson) {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path SUITE_ROOT = Path.of("src/test/resources/jsonata-js/test/test-suite");

    public static PerformanceCaseFixture load(String caseName) throws IOException {
        var caseFile = SUITE_ROOT.resolve("groups/performance/" + caseName + ".json");
        var caseNode = MAPPER.readTree(caseFile.toFile());
        var datasetName = caseNode.get("dataset").asText();
        var datasetFile = SUITE_ROOT.resolve("datasets/" + datasetName + ".json");
        var inputJson = Files.readString(datasetFile);
        return new PerformanceCaseFixture(caseName, caseNode.get("expr").asText(), inputJson);
    }
}
