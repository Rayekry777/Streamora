package com.streamora.admin.application;

/** Stable authorization or CSRF failure for the administrator API. */
public class AdminForbiddenException extends RuntimeException {

    private final String code;

    public AdminForbiddenException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
