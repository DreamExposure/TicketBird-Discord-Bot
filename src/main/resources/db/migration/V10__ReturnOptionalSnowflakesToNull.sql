UPDATE guild_settings
    SET awaiting_category = null
    WHERE awaiting_category = 0;

UPDATE guild_settings
    SET responded_category = null
    WHERE responded_category = 0;

UPDATE guild_settings
    SET hold_category = null
    WHERE hold_category = 0;

UPDATE guild_settings
    SET close_category = null
    WHERE close_category = 0;

UPDATE guild_settings
    SET support_channel = null
    WHERE support_channel = 0;

UPDATE guild_settings
    SET static_message = null
    WHERE static_message = 0;
