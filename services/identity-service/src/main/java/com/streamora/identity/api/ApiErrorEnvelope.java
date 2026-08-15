package com.streamora.identity.api;

import java.util.List;

/** Standard Streamora error envelope. */
public record ApiErrorEnvelope(ApiError error, String requestId) {

    public record ApiError(String code, String message, List<FieldError> details) {
    }

    public record FieldError(String field, String reason) {
    }
}
