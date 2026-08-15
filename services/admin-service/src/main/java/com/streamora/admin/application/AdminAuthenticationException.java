package com.streamora.admin.application;

/** Stable authentication failure for the administrator API. */
public class AdminAuthenticationException extends RuntimeException {

    private final String code;

    public AdminAuthenticationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
