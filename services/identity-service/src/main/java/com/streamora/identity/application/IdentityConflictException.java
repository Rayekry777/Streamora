package com.streamora.identity.application;

/** Stable conflict used for unique identity attributes. */
public class IdentityConflictException extends RuntimeException {

    private final String code;

    public IdentityConflictException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
