package com.streamora.video.api;

/** Standard external API success envelope. */
public record ApiEnvelope<T>(T data, String requestId) {
}
