ALTER TABLE guild_settings
    ADD COLUMN staff_role BIGINT NULL DEFAULT NULL
        AFTER staff;
