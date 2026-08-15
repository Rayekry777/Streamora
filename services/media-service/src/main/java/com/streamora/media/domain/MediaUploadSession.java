package com.streamora.media.domain;

import java.time.Instant;
import java.util.List;

/** Upload session returned without object-store credentials. */
public record MediaUploadSession(
        String uploadId,
        String assetId,
        long partSizeBytes,
        Instant expiresAt,
        List<UploadPartUrl> parts) {
    public record UploadPartUrl(int partNumber, String url, Instant expiresAt) {
    }
}
