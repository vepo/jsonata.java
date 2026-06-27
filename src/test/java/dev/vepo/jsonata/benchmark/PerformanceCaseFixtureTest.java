package dev.vepo.jsonata.benchmark;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PerformanceCaseFixtureTest {

    @Test
    void loadsPerformanceCasesFromJsonataJsSuite() throws Exception {
        var case000 = PerformanceCaseFixture.load("case000");
        assertThat(case000.expr()).contains("items");
        assertThat(case000.inputJson()).contains("\"items\"");

        var case001 = PerformanceCaseFixture.load("case001");
        assertThat(case001.expr()).contains("$i");
        assertThat(case001.inputJson()).contains("\"label_0\"");
    }
}
