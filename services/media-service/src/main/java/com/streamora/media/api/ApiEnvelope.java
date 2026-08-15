package com.streamora.media.api;

public record ApiEnvelope<T>(T data, String requestId) {
}
