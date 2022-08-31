ALTER TABLE guild_settings
    ADD COLUMN auto_close_hours INT NOT NULL DEFAULT 168 -- 1 week
        AFTER use_projects;

ALTER TABLE guild_settings
    ADD COLUMN auto_delete_hours INT NOT NULL DEFAULT 24 -- 1 day
        AFTER auto_close_hours;
