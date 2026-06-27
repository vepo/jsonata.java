package dev.vepo.jsonata;

import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Resource limits for expression evaluation (mirrors jsonata-js guardrails).
 */
public record Guardrails(OptionalLong timeoutMs, OptionalInt maxStackDepth, OptionalInt maxSequenceLength) {

    public static Guardrails none() {
        return new Guardrails(OptionalLong.empty(), OptionalInt.empty(), OptionalInt.empty());
    }

    public static Guardrails of(long timeoutMs) {
        return new Guardrails(OptionalLong.of(timeoutMs), OptionalInt.empty(), OptionalInt.empty());
    }

    public static Guardrails depth(int maxDepth) {
        return new Guardrails(OptionalLong.empty(), OptionalInt.of(maxDepth), OptionalInt.empty());
    }
}
