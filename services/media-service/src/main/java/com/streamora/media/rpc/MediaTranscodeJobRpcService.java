package com.streamora.media.rpc;

import com.streamora.contract.media.v1.ClaimNextTranscodeJobRequest;
import com.streamora.contract.media.v1.ClaimedTranscodeJob;
import com.streamora.contract.media.v1.CompleteTranscodeJobCommand;
import com.streamora.contract.media.v1.FailTranscodeJobCommand;
import com.streamora.contract.media.v1.MediaTranscodeJobService;
import com.streamora.contract.media.v1.TranscodeJobMutationResult;
import java.util.concurrent.CompletableFuture;
import org.apache.dubbo.config.annotation.DubboService;

/** Protobuf-defined Dubbo Triple provider for worker job leases and result reporting. */
@DubboService(interfaceClass = MediaTranscodeJobService.class, version = "1.0.0")
public class MediaTranscodeJobRpcService implements MediaTranscodeJobService {
    private final com.streamora.media.application.MediaTranscodeJobService transcodeJobService;

    public MediaTranscodeJobRpcService(com.streamora.media.application.MediaTranscodeJobService transcodeJobService) {
        this.transcodeJobService = transcodeJobService;
    }

    @Override
    public ClaimedTranscodeJob claimNextJob(ClaimNextTranscodeJobRequest request) {
        return transcodeJobService.claimNext(request.getWorkerId())
                .map(job -> ClaimedTranscodeJob.newBuilder()
                        .setClaimed(true)
                        .setJobId(job.jobId())
                        .setAssetId(job.assetId())
                        .setSourceObjectKey(job.sourceObjectKey())
                        .setOutputPrefix(job.outputPrefix())
                        .setClaimToken(job.claimToken())
                        .setAttemptCount(job.attemptCount())
                        .build())
                .orElseGet(() -> ClaimedTranscodeJob.newBuilder().setClaimed(false).build());
    }

    @Override
    public CompletableFuture<ClaimedTranscodeJob> claimNextJobAsync(ClaimNextTranscodeJobRequest request) {
        return CompletableFuture.completedFuture(claimNextJob(request));
    }

    @Override
    public TranscodeJobMutationResult completeJob(CompleteTranscodeJobCommand request) {
        boolean completed = transcodeJobService.complete(request.getJobId(), request.getAssetId(), request.getClaimToken(),
                request.getManifestObjectKey(), request.getPosterObjectKey(), request.getTraceId());
        return mutationResult(completed);
    }

    @Override
    public CompletableFuture<TranscodeJobMutationResult> completeJobAsync(CompleteTranscodeJobCommand request) {
        return CompletableFuture.completedFuture(completeJob(request));
    }

    @Override
    public TranscodeJobMutationResult failJob(FailTranscodeJobCommand request) {
        boolean completed = transcodeJobService.fail(request.getJobId(), request.getAssetId(), request.getClaimToken(),
                request.getFailureCode());
        return mutationResult(completed);
    }

    @Override
    public CompletableFuture<TranscodeJobMutationResult> failJobAsync(FailTranscodeJobCommand request) {
        return CompletableFuture.completedFuture(failJob(request));
    }

    private static TranscodeJobMutationResult mutationResult(boolean completed) {
        return TranscodeJobMutationResult.newBuilder()
                .setCompleted(completed)
                .setErrorCode(completed ? "" : "TRANSCODE_JOB_LEASE_INVALID")
                .build();
    }
}
