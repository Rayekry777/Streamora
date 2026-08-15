package com.streamora.media.domain;

/** Immutable lease granted by media-service to one transcode worker. */
public record ClaimedTranscodeJob(
        String jobId,
        String assetId,
        String sourceObjectKey,
        String outputPrefix,
        String claimToken,
        int attemptCount) {
}
