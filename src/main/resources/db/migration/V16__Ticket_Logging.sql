ALTER TABLE guild_settings
    ADD COLUMN log_channel BIGINT NULL
    AFTER support_channel;

ALTER TABLE guild_settings
    ADD COLUMN enable_logging BOOLEAN NOT NULL DEFAULT 0
    AFTER USE_PROJECTS;
