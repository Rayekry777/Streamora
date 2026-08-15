package com.streamora.admin.identity;

/** Indicates that identity-service cannot currently serve the admin request. */
public class IdentityUnavailableException extends RuntimeException {

    public IdentityUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdentityUnavailableException(String message) {
        super(message);
    }
}
