package com.streamora.media.domain;

/** Browser-reported object-store part result. */
public record CompletedPart(int partNumber, String etag) {
}
