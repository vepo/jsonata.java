package dev.vepo.jsonata.exception;

public class JSONataException extends RuntimeException {
    public JSONataException(String message, Exception cause) {
        super(message, cause);
    }
}
