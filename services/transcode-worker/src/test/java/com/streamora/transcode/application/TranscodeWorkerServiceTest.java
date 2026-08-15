package com.streamora.transcode.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.streamora.transcode.domain.PublishedTranscodeArtifacts;
import com.streamora.transcode.domain.TranscodeArtifacts;
import com.streamora.transcode.domain.TranscodeJob;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TranscodeWorkerServiceTest {
    @Test
    void shouldCompleteClaimedJobWithoutOwningMediaState() {
        var job = new TranscodeJob("job-1", "asset-1", "original/100/asset-1/source", "hls/asset-1/", "lease-1", 1);
        var client = new RecordingJobClient(job);
        var workspace = new TranscodeWorkspace() {
            @Override
            public Path materializeSource(TranscodeJob ignored) {
                return Path.of("source.mp4");
            }

            @Override
            public PublishedTranscodeArtifacts publish(TranscodeJob ignored, TranscodeArtifacts artifacts) {
                return new PublishedTranscodeArtifacts("hls/asset-1/master.m3u8", "hls/asset-1/poster.jpg");
            }
        };
        var executor = (HlsTranscodeExecutor) (ignored, source) -> new TranscodeArtifacts(Path.of("artifacts"),
                Path.of("artifacts/master.m3u8"), Path.of("artifacts/poster.jpg"));

        new TranscodeWorkerService(client, workspace, executor, "worker-a").processOne();

        assertThat(client.completed).isTrue();
        assertThat(client.failed).isFalse();
    }

    private static final class RecordingJobClient implements TranscodeJobClient {
        private final TranscodeJob job;
        private boolean claimed;
        private boolean completed;
        private boolean failed;

        private RecordingJobClient(TranscodeJob job) {
            this.job = job;
        }

        @Override
        public Optional<TranscodeJob> claimNext(String workerId, String traceId) {
            if (claimed) {
                return Optional.empty();
            }
            claimed = true;
            return Optional.of(job);
        }

        @Override
        public boolean complete(TranscodeJob ignored, PublishedTranscodeArtifacts artifacts, String traceId) {
            completed = true;
            return true;
        }

        @Override
        public boolean fail(TranscodeJob ignored, String failureCode, String traceId) {
            failed = true;
            return true;
        }
    }
}
