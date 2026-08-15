package com.streamora.media.infrastructure;

import com.streamora.media.domain.CompletedPart;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Local-only test double; production replaces this with the S3 adapter. */
@Component
@ConditionalOnProperty(name = "streamora.media.object-store.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryMultipartObjectStore implements MultipartObjectStore {
    @Override
    public String createMultipartUpload(String objectKey, String contentType) {
        return UUID.randomUUID().toString();
    }

    @Override
    public String presignPart(String objectKey, String objectStoreUploadId, int partNumber, Instant expiresAt) {
        return "memory://uploads/" + objectStoreUploadId + "/parts/" + partNumber;
    }

    @Override
    public void completeMultipartUpload(String objectKey, String objectStoreUploadId, List<CompletedPart> parts) {
        // The local test double validates sequencing in the application service only.
    }
}
