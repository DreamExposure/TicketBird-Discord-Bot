# noinspection SqlResolveForFile

ALTER TABLE ${prefix}guild_settings
    ADD COLUMN USE_PROJECTS BIT(1) NOT NULL DEFAULT 0
        AFTER DEV_GUILD;
