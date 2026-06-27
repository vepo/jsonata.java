package dev.vepo.jsonata.conformance;

import java.util.Set;

/**
 * Cases skipped until features are implemented or deemed out of scope.
 */
public final class ConformanceSkipList {

    private static final Set<String> SKIPPED = Set.of(
            // Timing benchmarks — not correctness
            "performance/*",
            // Reference-implementation internals
            "token-conversion/*");

    private ConformanceSkipList() {
    }

    public static boolean isSkipped(String group, String caseName) {
        var key = group + "/" + caseName;
        return SKIPPED.stream().anyMatch(pattern -> matches(pattern, key));
    }

    private static boolean matches(String pattern, String key) {
        if (pattern.endsWith("/*")) {
            return key.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return pattern.equals(key);
    }
}
