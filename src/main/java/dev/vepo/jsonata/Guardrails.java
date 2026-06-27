package dev.vepo.jsonata;

import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Optional resource limits applied during expression evaluation.
 * <p>
 * Layer: <strong>application</strong>. Configured on {@link EvaluationEnvironment} and enforced
 * by {@link dev.vepo.jsonata.functions.EvaluationContext}; mirrors jsonata-js guardrail options.
 * <p>
 * Each limit is independent; empty optionals mean no limit for that dimension.
 * Invariants: instances are immutable records; {@link #none()} is the default everywhere.
 */
public record Guardrails(OptionalLong timeoutMs, OptionalInt maxStackDepth, OptionalInt maxSequenceLength) {

    /**
     * No limits on evaluation time, stack depth, or sequence length.
     *
     * @return guardrails with all dimensions unset
     */
    public static Guardrails none() {
        return new Guardrails(OptionalLong.empty(), OptionalInt.empty(), OptionalInt.empty());
    }

    /**
     * Limits wall-clock evaluation time.
     *
     * @param timeoutMs maximum milliseconds before evaluation is aborted
     * @return guardrails with only timeout configured
     */
    public static Guardrails of(long timeoutMs) {
        return new Guardrails(OptionalLong.of(timeoutMs), OptionalInt.empty(), OptionalInt.empty());
    }

    /**
     * Limits recursive evaluation depth (stack depth).
     *
     * @param maxDepth maximum nested call depth allowed
     * @return guardrails with only stack depth configured
     */
    public static Guardrails depth(int maxDepth) {
        return new Guardrails(OptionalLong.empty(), OptionalInt.of(maxDepth), OptionalInt.empty());
    }
}
