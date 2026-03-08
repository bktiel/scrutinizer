package com.scrutinizer.policy;

/**
 * Raised when a YAML policy file is malformed or contains invalid configuration.
 */
public class PolicyParseException extends RuntimeException {

    public PolicyParseException(String message) {
        super(message);
    }

    public PolicyParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
