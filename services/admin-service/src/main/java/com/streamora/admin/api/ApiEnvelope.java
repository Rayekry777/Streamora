package com.streamora.admin.api;

/** Standard Streamora success envelope. */
public record ApiEnvelope<T>(T data, String requestId) {
}
