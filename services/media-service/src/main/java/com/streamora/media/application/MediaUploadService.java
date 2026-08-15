package com.streamora.media.application;

import com.streamora.media.domain.CompletedPart;
import com.streamora.media.domain.MediaUploadCompletion;
import com.streamora.media.domain.MediaUploadSession;
import com.streamora.media.infrastructure.MediaUploadRepository;
import com.streamora.media.infrastructure.MediaUploadRepository.StoredUpload;
import com.streamora.media.infrastructure.MultipartObjectStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates direct multipart upload sessions and atomically queues completed assets for transcoding. */
@Service
public class MediaUploadService {
    private final MediaUploadRepository repository;
    private final MultipartObjectStore objectStore;
    private final long partSizeBytes;
    private final Duration urlTtl;
    private final Clock clock = Clock.systemUTC();

    public MediaUploadService(
            MediaUploadRepository repository,
            MultipartObjectStore objectStore,
            @Value("${streamora.media.upload.part-size-bytes}") long partSizeBytes,
            @Value("${streamora.media.upload.url-ttl-seconds}") long urlTtlSeconds) {
        this.repository = repository;
        this.objectStore = objectStore;
        this.partSizeBytes = partSizeBytes;
        this.urlTtl = Duration.ofSeconds(urlTtlSeconds);
    }

    @Transactional
    public MediaUploadSession create(String ownerSubjectId, String fileName, String contentType, long totalBytes, String idempotencyKey) {
        StoredUpload existing = repository.findByIdempotencyKey(ownerSubjectId, idempotencyKey).orElse(null);
        if (existing != null) {
            return session(existing);
        }
        Instant now = clock.instant();
        Instant expiresAt = now.plus(urlTtl);
        int expectedParts = Math.toIntExact((totalBytes + partSizeBytes - 1) / partSizeBytes);
        String assetId = UUID.randomUUID().toString();
        String objectKey = "original/" + ownerSubjectId + "/" + assetId + "/source";
        String storeUploadId = objectStore.createMultipartUpload(objectKey, contentType);
        StoredUpload upload = new StoredUpload(UUID.randomUUID().toString(), assetId, objectKey, storeUploadId,
                partSizeBytes, expectedParts, expiresAt, "OPEN", null, idempotencyKey);
        repository.create(upload, ownerSubjectId, fileName, contentType, totalBytes, now);
        return session(upload);
    }

    @Transactional
    public MediaUploadCompletion complete(String ownerSubjectId, String uploadId, String idempotencyKey, List<CompletedPart> parts) {
        StoredUpload upload = repository.findById(ownerSubjectId, uploadId)
                .orElseThrow(() -> new MediaUploadException("MEDIA_UPLOAD_NOT_FOUND", "上传会话不存在或不可访问"));
        if (!idempotencyKey.equals(upload.idempotencyKey())) {
            throw new MediaUploadException("IDEMPOTENCY_KEY_MISMATCH", "幂等键与上传会话不匹配");
        }
        if (upload.expiresAt().isBefore(clock.instant()) && !"COMPLETED".equals(upload.status())) {
            throw new MediaUploadException("MEDIA_UPLOAD_EXPIRED", "上传会话已过期");
        }
        validateParts(parts, upload.expectedPartCount());
        if (!"COMPLETED".equals(upload.status())) {
            objectStore.completeMultipartUpload(upload.objectKey(), upload.objectStoreUploadId(), parts);
        }
        return repository.markCompleted(upload, clock.instant());
    }

    private MediaUploadSession session(StoredUpload upload) {
        List<MediaUploadSession.UploadPartUrl> parts = java.util.stream.IntStream.rangeClosed(1, upload.expectedPartCount())
                .mapToObj(part -> new MediaUploadSession.UploadPartUrl(part,
                        objectStore.presignPart(upload.objectKey(), upload.objectStoreUploadId(), part, upload.expiresAt()),
                        upload.expiresAt()))
                .toList();
        return new MediaUploadSession(upload.uploadId(), upload.assetId(), upload.partSizeBytes(), upload.expiresAt(), parts);
    }

    private static void validateParts(List<CompletedPart> parts, int expectedPartCount) {
        if (parts.size() != expectedPartCount) {
            throw new MediaUploadException("MEDIA_UPLOAD_PARTS_INCOMPLETE", "上传分片数量不完整");
        }
        for (int index = 0; index < parts.size(); index++) {
            CompletedPart part = parts.get(index);
            if (part.partNumber() != index + 1 || part.etag() == null || part.etag().isBlank()) {
                throw new MediaUploadException("MEDIA_UPLOAD_PARTS_INVALID", "上传分片顺序或 ETag 无效");
            }
        }
    }
}
