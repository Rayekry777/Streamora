package com.streamora.transcode.application;

import com.streamora.transcode.domain.PublishedTranscodeArtifacts;
import com.streamora.transcode.domain.TranscodeJob;
import java.util.Optional;

/** Port to the media-service, which remains the owner of job state. */
public interface TranscodeJobClient {
    Optional<TranscodeJob> claimNext(String workerId, String traceId);

    boolean complete(TranscodeJob job, PublishedTranscodeArtifacts artifacts, String traceId);

    boolean fail(TranscodeJob job, String failureCode, String traceId);
}
