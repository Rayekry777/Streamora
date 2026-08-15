package com.streamora.media.infrastructure;

import com.streamora.media.domain.CompletedPart;
import java.time.Instant;
import java.util.List;

/** Port for an S3-compatible multipart object store. */
public interface MultipartObjectStore {
    String createMultipartUpload(String objectKey, String contentType);

    String presignPart(String objectKey, String objectStoreUploadId, int partNumber, Instant expiresAt);

    void completeMultipartUpload(String objectKey, String objectStoreUploadId, List<CompletedPart> parts);
}
