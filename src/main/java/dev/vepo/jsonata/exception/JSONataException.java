package dev.vepo.jsonata.exception;

import java.util.Optional;

public class JSONataException extends RuntimeException {

    private final String code;

    public JSONataException(String message, Exception cause) {
        super(message, cause);
        this.code = null;
    }

    public JSONataException(String message) {
        super(message);
        this.code = null;
    }

    public JSONataException(String code, String message) {
        super(message);
        this.code = code;
    }

    public Optional<String> code() {
        return Optional.ofNullable(code);
    }
}
