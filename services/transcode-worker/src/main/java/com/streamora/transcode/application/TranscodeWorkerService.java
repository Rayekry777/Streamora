package com.streamora.transcode.application;

import com.streamora.transcode.domain.TranscodeJob;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Executes one leased task at a time and reports only idempotent terminal mutations. */
@Service
public class TranscodeWorkerService {
    private static final Logger log = LoggerFactory.getLogger(TranscodeWorkerService.class);

    private final TranscodeJobClient jobClient;
    private final TranscodeWorkspace workspace;
    private final HlsTranscodeExecutor executor;
    private final String workerId;

    public TranscodeWorkerService(TranscodeJobClient jobClient, TranscodeWorkspace workspace, HlsTranscodeExecutor executor,
                                  @Value("${streamora.transcode.worker-id}") String workerId) {
        this.jobClient = jobClient;
        this.workspace = workspace;
        this.executor = executor;
        this.workerId = workerId;
    }

    public void processOne() {
        String traceId = UUID.randomUUID().toString();
        jobClient.claimNext(workerId, traceId).ifPresent(job -> process(job, traceId));
    }

    private void process(TranscodeJob job, String traceId) {
        try {
            var source = workspace.materializeSource(job);
            var artifacts = executor.transcode(job, source);
            var published = workspace.publish(job, artifacts);
            if (!jobClient.complete(job, published, traceId)) {
                log.warn("Transcode completion lease was no longer valid for job {}", job.jobId());
            }
        } catch (RuntimeException exception) {
            log.warn("Transcode job {} failed: {}", job.jobId(), exception.getMessage());
            if (!jobClient.fail(job, "TRANSCODE_EXECUTION_FAILED", traceId)) {
                log.warn("Transcode failure lease was no longer valid for job {}", job.jobId());
            }
        }
    }
}
