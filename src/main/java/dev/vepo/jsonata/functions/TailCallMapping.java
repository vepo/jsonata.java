package dev.vepo.jsonata.functions;

import dev.vepo.jsonata.functions.data.Data;

/**
 * Body mapping that supports tail-call evaluation with invocation frame.
 */
public interface TailCallMapping extends Mapping {

    Data mapWithFrame(Data original, Data current, BlockContext frame);
}
