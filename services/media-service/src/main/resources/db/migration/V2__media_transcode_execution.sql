ALTER TABLE media.transcode_job ADD COLUMN claim_token VARCHAR(64);
ALTER TABLE media.transcode_job ADD COLUMN claimed_by VARCHAR(128);
ALTER TABLE media.transcode_job ADD COLUMN claimed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE media.transcode_job ADD COLUMN claim_expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE media.transcode_job ADD COLUMN completed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE media.transcode_job ADD COLUMN failure_code VARCHAR(128);
ALTER TABLE media.transcode_job ADD COLUMN hls_manifest_key VARCHAR(512);
ALTER TABLE media.transcode_job ADD COLUMN poster_key VARCHAR(512);

ALTER TABLE media.outbox_event ADD COLUMN schema_version INTEGER NOT NULL DEFAULT 1;
ALTER TABLE media.outbox_event ADD COLUMN trace_id VARCHAR(128) NOT NULL DEFAULT 'system';

CREATE INDEX idx_transcode_job_claim ON media.transcode_job(status, claim_expires_at, created_at);
