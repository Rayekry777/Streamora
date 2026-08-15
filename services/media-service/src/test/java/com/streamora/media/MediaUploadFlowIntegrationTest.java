package com.streamora.media;

import static org.assertj.core.api.Assertions.assertThat;

import com.streamora.media.application.MediaUploadService;
import com.streamora.media.domain.CompletedPart;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class MediaUploadFlowIntegrationTest {
    private static final String OWNER = "10001";
    private static final String KEY = "upload-idempotency-key-0001";

    @Autowired
    private MediaUploadService uploadService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateOneUploadAndQueueOneTranscodeJobAfterIdempotentCompletion() {
        var first = uploadService.create(OWNER, "pet-video.mp4", "video/mp4", 8_388_609L, KEY);
        var retry = uploadService.create(OWNER, "pet-video.mp4", "video/mp4", 8_388_609L, KEY);

        assertThat(retry.uploadId()).isEqualTo(first.uploadId());
        assertThat(first.parts()).hasSize(2);

        var completion = uploadService.complete(OWNER, first.uploadId(), KEY,
                java.util.List.of(new CompletedPart(1, "etag-1"), new CompletedPart(2, "etag-2")));
        var repeatedCompletion = uploadService.complete(OWNER, first.uploadId(), KEY,
                java.util.List.of(new CompletedPart(1, "etag-1"), new CompletedPart(2, "etag-2")));

        assertThat(repeatedCompletion.transcodeJobId()).isEqualTo(completion.transcodeJobId());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM media.transcode_job WHERE asset_id = ?", Integer.class, first.assetId())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM media.outbox_event WHERE aggregate_id = ?", Integer.class, first.assetId())).isEqualTo(1);
    }
}
