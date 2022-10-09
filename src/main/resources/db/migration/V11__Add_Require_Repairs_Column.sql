ALTER TABLE guild_settings
    ADD COLUMN requires_repair BIT(1) NOT NULL DEFAULT 0
        AFTER use_projects;
