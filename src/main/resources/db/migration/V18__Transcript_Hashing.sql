ALTER TABLE tickets
    ADD COLUMN transcript_sha256 VARCHAR(64) NULL DEFAULT NULL
    AFTER last_activity;

ALTER TABLE tickets
    ADD COLUMN attachments_sha256 VARCHAR(64) NULL DEFAULT NULL
    AFTER transcript_sha256;
