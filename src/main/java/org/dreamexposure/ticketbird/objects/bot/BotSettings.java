package org.dreamexposure.ticketbird.objects.bot;

import java.util.Properties;

public enum BotSettings {
    SQL_HOST, SQL_USER, SQL_PASSWORD,
    SQL_DB, SQL_PORT, SQL_PREFIX,

    REDIS_HOSTNAME, REDIS_PORT,
    REDIS_PASSWORD,

    LOG_FOLDER, LANG_PATH,

    TOKEN, SECRET, ID,
    PW_TOKEN, DBO_TOKEN,

    UPDATE_SITES, RUN_API, USE_REDIS_STORES, USE_WEBHOOKS,

    SHARD_COUNT, SHARD_INDEX,

    DEBUG_WEBHOOK, ERROR_WEBHOOK, STATUS_WEBHOOK,

    PORT;

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