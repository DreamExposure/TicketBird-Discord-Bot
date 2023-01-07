ALTER TABLE guild_settings
    ADD COLUMN ping_option SMALLINT NOT NULL DEFAULT 1
        AFTER staff_role;

ALTER TABLE projects
    ADD COLUMN ping_override SMALLINT NOT NULL DEFAULT 1
        AFTER staff_roles;

