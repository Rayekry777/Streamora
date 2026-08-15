CREATE SCHEMA IF NOT EXISTS media;

CREATE TABLE media.media_asset (
    id VARCHAR(36) PRIMARY KEY,
    owner_subject_id VARCHAR(40) NOT NULL,
    object_key VARCHAR(512) NOT NULL UNIQUE,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    total_bytes BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE media.multipart_upload (
    id VARCHAR(36) PRIMARY KEY,
    asset_id VARCHAR(36) NOT NULL UNIQUE,
    owner_subject_id VARCHAR(40) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    object_store_upload_id VARCHAR(128) NOT NULL,
    part_size_bytes BIGINT NOT NULL,
    expected_part_count INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_multipart_upload_owner_key UNIQUE (owner_subject_id, idempotency_key)
);

CREATE TABLE media.transcode_job (
    id VARCHAR(36) PRIMARY KEY,
    asset_id VARCHAR(36) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE media.outbox_event (
    id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    payload_json CLOB NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_multipart_upload_owner ON media.multipart_upload(owner_subject_id, status);
CREATE INDEX idx_transcode_job_status ON media.transcode_job(status, created_at);
