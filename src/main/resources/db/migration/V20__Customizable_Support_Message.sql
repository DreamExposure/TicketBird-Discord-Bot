ALTER TABLE guild_settings
    ADD COLUMN static_message_title TEXT NULL DEFAULT NULL
    AFTER static_message;

ALTER TABLE guild_settings
    ADD COLUMN static_message_description TEXT NULL DEFAULT NULL
    AFTER static_message_title;
