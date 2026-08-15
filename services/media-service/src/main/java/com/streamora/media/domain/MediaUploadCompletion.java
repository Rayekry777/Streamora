package com.streamora.media.domain;

/** Idempotent completion result after a transcode job has been persisted. */
public record MediaUploadCompletion(String uploadId, String assetId, String transcodeJobId, String status) {
}
