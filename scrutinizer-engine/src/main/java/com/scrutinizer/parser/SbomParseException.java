package com.scrutinizer.parser;

/**
 * Raised when the SBOM JSON is malformed or missing required fields.
 */
public class SbomParseException extends RuntimeException {

    public SbomParseException(String message) {
        super(message);
    }

    public SbomParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
