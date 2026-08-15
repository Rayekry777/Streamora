package com.streamora.identity.api;

/** Protocol-level access denial such as failed CSRF validation. */
public class IdentityForbiddenException extends RuntimeException {

    private final String code;

    public IdentityForbiddenException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
