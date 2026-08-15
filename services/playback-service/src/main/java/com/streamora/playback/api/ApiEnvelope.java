package com.streamora.playback.api;

public record ApiEnvelope<T>(T data, String requestId) {
}
