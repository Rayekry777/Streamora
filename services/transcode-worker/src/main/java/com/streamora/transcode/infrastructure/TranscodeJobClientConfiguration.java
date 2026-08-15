package com.streamora.transcode.infrastructure;

import com.streamora.transcode.application.TranscodeJobClient;
import com.streamora.transcode.domain.PublishedTranscodeArtifacts;
import com.streamora.transcode.domain.TranscodeJob;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Keeps local health checks infrastructure-free. */
@Configuration
public class TranscodeJobClientConfiguration {
    @Bean
    @ConditionalOnMissingBean(TranscodeJobClient.class)
    TranscodeJobClient noOpTranscodeJobClient() {
        return new TranscodeJobClient() {
            @Override
            public Optional<TranscodeJob> claimNext(String workerId, String traceId) {
                return Optional.empty();
            }

            @Override
            public boolean complete(TranscodeJob job, PublishedTranscodeArtifacts artifacts, String traceId) {
                return false;
            }

            @Override
            public boolean fail(TranscodeJob job, String failureCode, String traceId) {
                return false;
            }
        };
    }
}
