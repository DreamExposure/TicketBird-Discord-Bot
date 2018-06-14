package com.novamaday.ticketbird.objects.bot;

import java.util.Properties;

public enum BotSettings {
    SQL_HOST, SQL_USER, SQL_PASSWORD,
    SQL_DB, SQL_PORT, SQL_PREFIX, TOKEN, SECRET, ID,
    LANG_PATH, PW_TOKEN, DBO_TOKEN, UPDATE_SITES, LOG_FOLDER, RUN_API, PORT;

    private String val;

    BotSettings() {
    }

    public static void init(Properties properties) {
        for (BotSettings s : values()) {
            s.set(properties.getProperty(s.name()));
        }
    }

    public String get() {
        return val;
    }

    public void set(String val) {
        this.val = val;
    }
}