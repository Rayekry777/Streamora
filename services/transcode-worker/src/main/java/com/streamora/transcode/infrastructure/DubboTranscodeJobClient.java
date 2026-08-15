package com.streamora.transcode.infrastructure;

import com.streamora.contract.media.v1.ClaimNextTranscodeJobRequest;
import com.streamora.contract.media.v1.CompleteTranscodeJobCommand;
import com.streamora.contract.media.v1.FailTranscodeJobCommand;
import com.streamora.contract.media.v1.MediaTranscodeJobService;
import com.streamora.transcode.application.TranscodeJobClient;
import com.streamora.transcode.domain.PublishedTranscodeArtifacts;
import com.streamora.transcode.domain.TranscodeJob;
import java.util.Optional;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Dubbo Triple adapter; no media database credentials are available to this worker. */
@Component
@ConditionalOnProperty(name = "streamora.media.rpc-enabled", havingValue = "true")
public class DubboTranscodeJobClient implements TranscodeJobClient {
    @DubboReference(interfaceClass = MediaTranscodeJobService.class, version = "1.0.0", check = false, timeout = 5000)
    private MediaTranscodeJobService mediaTranscodeJobService;

    @Override
    public Optional<TranscodeJob> claimNext(String workerId, String traceId) {
        var result = mediaTranscodeJobService.claimNextJob(ClaimNextTranscodeJobRequest.newBuilder()
                .setWorkerId(workerId)
                .setTraceId(traceId)
                .build());
        if (!result.getClaimed()) {
            return Optional.empty();
        }
        return Optional.of(new TranscodeJob(result.getJobId(), result.getAssetId(), result.getSourceObjectKey(),
                result.getOutputPrefix(), result.getClaimToken(), result.getAttemptCount()));
    }

    @Override
    public boolean complete(TranscodeJob job, PublishedTranscodeArtifacts artifacts, String traceId) {
        return mediaTranscodeJobService.completeJob(CompleteTranscodeJobCommand.newBuilder()
                .setJobId(job.jobId())
                .setAssetId(job.assetId())
                .setClaimToken(job.claimToken())
                .setManifestObjectKey(artifacts.manifestObjectKey())
                .setPosterObjectKey(artifacts.posterObjectKey())
                .setTraceId(traceId)
                .build()).getCompleted();
    }

    @Override
    public boolean fail(TranscodeJob job, String failureCode, String traceId) {
        return mediaTranscodeJobService.failJob(FailTranscodeJobCommand.newBuilder()
                .setJobId(job.jobId())
                .setAssetId(job.assetId())
                .setClaimToken(job.claimToken())
                .setFailureCode(failureCode)
                .setTraceId(traceId)
                .build()).getCompleted();
    }
}
