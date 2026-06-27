package dev.vepo.jsonata.functions;

import java.util.List;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Marker for tail-call optimization trampoline.
 */
public record TailCallThunk(Object target, List<Mapping> args, Data original, Data current) {
}
