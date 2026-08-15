package com.streamora.identity.application;

/** Stable authentication failure without credential-specific disclosure. */
public class IdentityAuthenticationException extends RuntimeException {

    private final String code;

    public IdentityAuthenticationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
