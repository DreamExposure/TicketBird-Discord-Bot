ALTER TABLE `${prefix}guild_settings`
    ADD USE_PROJECTS BOOLEAN NOT NULL
        DEFAULT (true) AFTER 'DEV_GUILD';
