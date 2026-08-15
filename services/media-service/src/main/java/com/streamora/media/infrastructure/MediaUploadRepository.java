package com.streamora.media.infrastructure;

import com.streamora.media.domain.MediaUploadCompletion;
import com.streamora.media.domain.MediaUploadSession;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** JDBC persistence owned exclusively by media-service. */
@Repository
public class MediaUploadRepository {
    private final JdbcTemplate jdbcTemplate;

    public MediaUploadRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<StoredUpload> findByIdempotencyKey(String ownerSubjectId, String idempotencyKey) {
        return jdbcTemplate.query("""
                        SELECT u.id, u.asset_id, a.object_key, u.object_store_upload_id, u.idempotency_key, u.part_size_bytes,
                               u.expected_part_count, u.expires_at, u.status, j.id AS transcode_job_id
                        FROM media.multipart_upload u
                        JOIN media.media_asset a ON a.id = u.asset_id
                        LEFT JOIN media.transcode_job j ON j.asset_id = u.asset_id
                        WHERE u.owner_subject_id = ? AND u.idempotency_key = ?
                        """,
                (resultSet, rowNumber) -> new StoredUpload(
                        resultSet.getString("id"), resultSet.getString("asset_id"), resultSet.getString("object_key"),
                        resultSet.getString("object_store_upload_id"), resultSet.getLong("part_size_bytes"),
                        resultSet.getInt("expected_part_count"), resultSet.getTimestamp("expires_at").toInstant(),
                        resultSet.getString("status"), resultSet.getString("transcode_job_id"), resultSet.getString("idempotency_key")),
                ownerSubjectId, idempotencyKey).stream().findFirst();
    }

    public Optional<StoredUpload> findById(String ownerSubjectId, String uploadId) {
        return jdbcTemplate.query("""
                        SELECT u.id, u.asset_id, a.object_key, u.object_store_upload_id, u.idempotency_key, u.part_size_bytes,
                               u.expected_part_count, u.expires_at, u.status, j.id AS transcode_job_id
                        FROM media.multipart_upload u
                        JOIN media.media_asset a ON a.id = u.asset_id
                        LEFT JOIN media.transcode_job j ON j.asset_id = u.asset_id
                        WHERE u.owner_subject_id = ? AND u.id = ?
                        """,
                (resultSet, rowNumber) -> new StoredUpload(
                        resultSet.getString("id"), resultSet.getString("asset_id"), resultSet.getString("object_key"),
                        resultSet.getString("object_store_upload_id"), resultSet.getLong("part_size_bytes"),
                        resultSet.getInt("expected_part_count"), resultSet.getTimestamp("expires_at").toInstant(),
                        resultSet.getString("status"), resultSet.getString("transcode_job_id"), resultSet.getString("idempotency_key")),
                ownerSubjectId, uploadId).stream().findFirst();
    }

    public void create(StoredUpload upload, String ownerSubjectId, String fileName, String contentType, long totalBytes, Instant now) {
        jdbcTemplate.update("""
                        INSERT INTO media.media_asset
                        (id, owner_subject_id, object_key, original_file_name, content_type, total_bytes, status, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, 'UPLOADING', ?, ?)
                        """, upload.assetId(), ownerSubjectId, upload.objectKey(), fileName, contentType, totalBytes,
                Timestamp.from(now), Timestamp.from(now));
        jdbcTemplate.update("""
                        INSERT INTO media.multipart_upload
                        (id, asset_id, owner_subject_id, idempotency_key, object_store_upload_id, part_size_bytes,
                         expected_part_count, status, expires_at, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'OPEN', ?, ?)
                        """, upload.uploadId(), upload.assetId(), ownerSubjectId, upload.idempotencyKey(),
                upload.objectStoreUploadId(), upload.partSizeBytes(), upload.expectedPartCount(),
                Timestamp.from(upload.expiresAt()), Timestamp.from(now));
    }

    public MediaUploadCompletion markCompleted(StoredUpload upload, Instant now) {
        if ("COMPLETED".equals(upload.status())) {
            return new MediaUploadCompletion(upload.uploadId(), upload.assetId(), upload.transcodeJobId(), "TRANSCODE_PENDING");
        }
        String jobId = UUID.randomUUID().toString();
        jdbcTemplate.update("UPDATE media.multipart_upload SET status = 'COMPLETED', completed_at = ? WHERE id = ?", Timestamp.from(now), upload.uploadId());
        jdbcTemplate.update("UPDATE media.media_asset SET status = 'UPLOADED', updated_at = ? WHERE id = ?", Timestamp.from(now), upload.assetId());
        jdbcTemplate.update("""
                        INSERT INTO media.transcode_job (id, asset_id, status, attempt_count, created_at, updated_at)
                        VALUES (?, ?, 'PENDING', 0, ?, ?)
                        """, jobId, upload.assetId(), Timestamp.from(now), Timestamp.from(now));
        jdbcTemplate.update("""
                        INSERT INTO media.outbox_event (id, event_type, aggregate_id, payload_json, occurred_at)
                        VALUES (?, 'media.upload.completed.v1', ?, ?, ?)
                        """, UUID.randomUUID().toString(), upload.assetId(), "{\"assetId\":\"" + upload.assetId() + "\"}", Timestamp.from(now));
        return new MediaUploadCompletion(upload.uploadId(), upload.assetId(), jobId, "TRANSCODE_PENDING");
    }

    /** Claims at most one pending or expired lease using a conditional update for cross-worker safety. */
    public Optional<ClaimedJob> claimNext(String workerId, String claimToken, Instant now, Instant leaseExpiresAt) {
        for (int retry = 0; retry < 3; retry++) {
            JobCandidate candidate = jdbcTemplate.query("""
                            SELECT j.id, j.asset_id, a.object_key, j.attempt_count
                            FROM media.transcode_job j
                            JOIN media.media_asset a ON a.id = j.asset_id
                            WHERE j.status = 'PENDING' OR (j.status = 'RUNNING' AND j.claim_expires_at < ?)
                            ORDER BY j.created_at
                            LIMIT 1
                            """,
                    (resultSet, rowNumber) -> new JobCandidate(resultSet.getString("id"), resultSet.getString("asset_id"),
                            resultSet.getString("object_key"), resultSet.getInt("attempt_count")),
                    Timestamp.from(now)).stream().findFirst().orElse(null);
            if (candidate == null) {
                return Optional.empty();
            }
            int updated = jdbcTemplate.update("""
                            UPDATE media.transcode_job
                            SET status = 'RUNNING', attempt_count = attempt_count + 1, claim_token = ?, claimed_by = ?,
                                claimed_at = ?, claim_expires_at = ?, failure_code = NULL, updated_at = ?
                            WHERE id = ? AND (status = 'PENDING' OR (status = 'RUNNING' AND claim_expires_at < ?))
                            """, claimToken, workerId, Timestamp.from(now), Timestamp.from(leaseExpiresAt), Timestamp.from(now),
                    candidate.jobId(), Timestamp.from(now));
            if (updated == 1) {
                return Optional.of(new ClaimedJob(candidate.jobId(), candidate.assetId(), candidate.sourceObjectKey(),
                        "hls/" + candidate.assetId() + "/", claimToken, candidate.attemptCount() + 1));
            }
        }
        return Optional.empty();
    }

    public boolean completeTranscode(String jobId, String assetId, String claimToken, String manifestObjectKey,
                                     String posterObjectKey, String traceId, Instant now) {
        int updated = jdbcTemplate.update("""
                        UPDATE media.transcode_job
                        SET status = 'COMPLETED', hls_manifest_key = ?, poster_key = ?, completed_at = ?,
                            claim_expires_at = NULL, updated_at = ?
                        WHERE id = ? AND asset_id = ? AND status = 'RUNNING' AND claim_token = ?
                        """, manifestObjectKey, posterObjectKey, Timestamp.from(now), Timestamp.from(now), jobId, assetId, claimToken);
        if (updated != 1) {
            return false;
        }
        jdbcTemplate.update("UPDATE media.media_asset SET status = 'TRANSCODED', updated_at = ? WHERE id = ?",
                Timestamp.from(now), assetId);
        jdbcTemplate.update("""
                        INSERT INTO media.outbox_event (id, event_type, aggregate_id, payload_json, occurred_at, schema_version, trace_id)
                        VALUES (?, 'media.asset.transcoded.v1', ?, ?, ?, 1, ?)
                        """, UUID.randomUUID().toString(), assetId,
                "{\"assetId\":\"" + assetId + "\",\"manifestObjectKey\":\"" + manifestObjectKey + "\",\"posterObjectKey\":\"" + posterObjectKey + "\"}",
                Timestamp.from(now), traceId);
        return true;
    }

    public boolean failTranscode(String jobId, String assetId, String claimToken, String failureCode, Instant now) {
        int updated = jdbcTemplate.update("""
                        UPDATE media.transcode_job
                        SET status = CASE WHEN attempt_count >= 3 THEN 'FAILED' ELSE 'PENDING' END,
                            failure_code = ?, claim_token = NULL, claimed_by = NULL, claimed_at = NULL,
                            claim_expires_at = NULL, updated_at = ?
                        WHERE id = ? AND asset_id = ? AND status = 'RUNNING' AND claim_token = ?
                        """, failureCode, Timestamp.from(now), jobId, assetId, claimToken);
        if (updated != 1) {
            return false;
        }
        Integer terminal = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM media.transcode_job WHERE id = ? AND status = 'FAILED'", Integer.class, jobId);
        if (terminal != null && terminal == 1) {
            jdbcTemplate.update("UPDATE media.media_asset SET status = 'TRANSCODE_FAILED', updated_at = ? WHERE id = ?",
                    Timestamp.from(now), assetId);
        }
        return true;
    }

    public record StoredUpload(String uploadId, String assetId, String objectKey, String objectStoreUploadId,
                               long partSizeBytes, int expectedPartCount, Instant expiresAt, String status,
                               String transcodeJobId, String idempotencyKey) {
        public StoredUpload(String uploadId, String assetId, String objectKey, String objectStoreUploadId,
                            long partSizeBytes, int expectedPartCount, Instant expiresAt, String status,
                            String transcodeJobId) {
            this(uploadId, assetId, objectKey, objectStoreUploadId, partSizeBytes, expectedPartCount, expiresAt, status,
                    transcodeJobId, null);
        }
    }

    private record JobCandidate(String jobId, String assetId, String sourceObjectKey, int attemptCount) {
    }

    public record ClaimedJob(String jobId, String assetId, String sourceObjectKey, String outputPrefix,
                             String claimToken, int attemptCount) {
    }
}
