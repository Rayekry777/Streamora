package com.streamora.media.application;

import com.streamora.media.domain.ClaimedTranscodeJob;
import com.streamora.media.infrastructure.MediaUploadRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates durable transcode-job leases without exposing the media database. */
@Service
public class MediaTranscodeJobService {
    private final MediaUploadRepository repository;
    private final Duration leaseDuration;
    private final Clock clock = Clock.systemUTC();

    public MediaTranscodeJobService(
            MediaUploadRepository repository,
            @Value("${streamora.media.transcode.lease-seconds}") long leaseSeconds) {
        this.repository = repository;
        this.leaseDuration = Duration.ofSeconds(leaseSeconds);
    }

    @Transactional
    public Optional<ClaimedTranscodeJob> claimNext(String workerId) {
        if (workerId == null || workerId.isBlank()) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        return repository.claimNext(workerId, UUID.randomUUID().toString(), now, now.plus(leaseDuration))
                .map(job -> new ClaimedTranscodeJob(job.jobId(), job.assetId(), job.sourceObjectKey(),
                        job.outputPrefix(), job.claimToken(), job.attemptCount()));
    }

    @Transactional
    public boolean complete(String jobId, String assetId, String claimToken, String manifestObjectKey,
                            String posterObjectKey, String traceId) {
        if (!isExpectedArtifactPrefix(assetId, manifestObjectKey) || !isExpectedArtifactPrefix(assetId, posterObjectKey)) {
            return false;
        }
        return repository.completeTranscode(jobId, assetId, claimToken, manifestObjectKey, posterObjectKey,
                traceId == null || traceId.isBlank() ? "system" : traceId, clock.instant());
    }

    @Transactional
    public boolean fail(String jobId, String assetId, String claimToken, String failureCode) {
        if (failureCode == null || failureCode.isBlank()) {
            return false;
        }
        return repository.failTranscode(jobId, assetId, claimToken, failureCode.trim(), clock.instant());
    }

    private static boolean isExpectedArtifactPrefix(String assetId, String objectKey) {
        return objectKey != null && objectKey.startsWith("hls/" + assetId + "/") && objectKey.length() <= 512;
    }
}
