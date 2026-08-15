package com.streamora.media.application;

/** Stable, client-safe media upload failure. */
public class MediaUploadException extends RuntimeException {
    private final String code;

    public MediaUploadException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
