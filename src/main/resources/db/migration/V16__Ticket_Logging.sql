ALTER TABLE guild_settings
    ADD COLUMN log_channel BIGINT NULL
    AFTER support_channel;
