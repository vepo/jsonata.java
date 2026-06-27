package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Deferred function call for tail-call trampoline execution.
 *
 * <p>Produced by {@link TailCallFunctionCall} and consumed by
 * {@link FunctionApplyService#resolveTailCall(TailCallThunk)}. Captures the call target,
 * unevaluated arguments, and evaluation contexts so the trampoline can re-enter apply
 * without consuming another stack frame.
 *
 * @param target   the function value or dynamic resolver
 * @param args     unevaluated argument expressions
 * @param original root input document at the call site
 * @param current  current focus at the call site
 */
public record TailCallThunk(Object target, List<Mapping> args, Data original, Data current) {
}
