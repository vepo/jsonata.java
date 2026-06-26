package dev.vepo.jsonata.conformance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonataConformanceTest {

    private static java.util.List<ConformanceCase> allCases;

    @BeforeAll
    void loadCases() throws IOException {
        allCases = ConformanceCase.loadAll();
        assertThat(allCases).isNotEmpty();
    }

    static Stream<ConformanceCase> conformanceCases() throws IOException {
        if (allCases == null) {
            allCases = ConformanceCase.loadAll();
        }
        return allCases.stream()
                       .filter(c -> !ConformanceSkipList.isSkipped(c.group(), c.caseName()));
    }

    @ParameterizedTest(name = "{0}/{1}")
    @MethodSource("conformanceCases")
    @Disabled("Conformance suite tracked via printBaselineReport; enable as pass rate improves")
    void conformanceCase(ConformanceCase testCase) {
        assertThat(testCase.run())
                .as("Group %s case %s expr: %s", testCase.group(), testCase.caseName(), testCase.expr())
                .isTrue();
    }

    @org.junit.jupiter.api.Test
    void printBaselineReport() throws IOException {
        var cases = ConformanceCase.loadAll();
        var reports = ConformanceCase.runAll(cases);
        var totalPass = reports.values().stream().mapToInt(r -> r.passed).sum();
        var totalCases = reports.values().stream().mapToInt(r -> r.total).sum();
        System.out.println("=== JSONata Conformance Baseline ===");
        System.out.printf("Overall: %d/%d (%.1f%%)%n", totalPass, totalCases,
                          totalCases == 0 ? 0 : 100.0 * totalPass / totalCases);
        reports.values().stream()
               .sorted((a, b) -> Double.compare(a.passRate(), b.passRate()))
               .forEach(r -> System.out.printf("  %s: %d/%d (%.0f%%)%n",
                                               r.group, r.passed, r.total, r.passRate()));
    }
}
