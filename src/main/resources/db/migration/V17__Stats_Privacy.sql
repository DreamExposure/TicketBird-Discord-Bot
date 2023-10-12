ALTER TABLE guild_settings
    ADD COLUMN show_ticket_stats BOOLEAN NOT NULL DEFAULT 1
    AFTER enable_logging;
