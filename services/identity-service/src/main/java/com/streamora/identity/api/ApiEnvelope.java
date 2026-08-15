package com.streamora.identity.api;

/** Standard Streamora success envelope. */
public record ApiEnvelope<T>(T data, String requestId) {
}
