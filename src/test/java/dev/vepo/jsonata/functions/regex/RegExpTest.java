package dev.vepo.jsonata.functions.regex;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RegExpTest {

    @Test
    void compileReturnsCachedInstanceForSamePattern() {
        var first = RegExp.compile("/test/i");
        var second = RegExp.compile("/test/i");

        assertThat(first).isSameAs(second);
    }
}
