package com.streamora.transcode.domain;

/** A leased media task. The worker never persists this state locally. */
public record TranscodeJob(
        String jobId,
        String assetId,
        String sourceObjectKey,
        String outputPrefix,
        String claimToken,
        int attemptCount) {
}
