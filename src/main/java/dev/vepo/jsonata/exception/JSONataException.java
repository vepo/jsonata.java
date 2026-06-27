package dev.vepo.jsonata.exception;

import java.util.Optional;

/**
 * Unchecked failure during JSONata parse or evaluation.
 * <p>
 * Layer: <strong>domain</strong> (cross-cutting error type). Thrown from the facade, parser,
 * and evaluation pipeline. An optional {@linkplain #code() machine-readable code} aligns with
 * jsonata-js error identifiers when present.
 * <p>
 * Invariants: always a {@link RuntimeException}; {@code code} is absent for generic failures.
 */
public class JSONataException extends RuntimeException {

    private final String code;

    /**
     * Wraps a lower-level failure with a descriptive message.
     *
     * @param message human-readable description
     * @param cause   underlying exception
     */
    public JSONataException(String message, Exception cause) {
        super(message, cause);
        this.code = null;
    }

    /**
     * Reports a parse or evaluation failure without a structured code.
     *
     * @param message human-readable description
     */
    public JSONataException(String message) {
        super(message);
        this.code = null;
    }

    /**
     * Reports a failure with a jsonata-js-compatible error code.
     *
     * @param code    machine-readable error identifier (e.g. syntax or type code)
     * @param message human-readable description
     */
    public JSONataException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Machine-readable error code when the failure is classified; empty for generic exceptions.
     *
     * @return optional error code
     */
    public Optional<String> code() {
        return Optional.ofNullable(code);
    }
}
