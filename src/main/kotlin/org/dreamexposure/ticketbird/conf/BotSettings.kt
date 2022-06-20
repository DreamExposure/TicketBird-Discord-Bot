package org.dreamexposure.ticketbird.conf

import java.util.*

enum class BotSettings {
    SQL_URL,
    SQL_PREFIX,

    REDIS_HOSTNAME,
    REDIS_PORT,
    REDIS_PASSWORD,

    LOG_FOLDER,

    TOKEN,
    SECRET,
    ID,

    GG_TOKEN,
    DBO_TOKEN,

    UPDATE_SITES,
    RUN_API,
    USE_REDIS_STORES,
    USE_WEBHOOKS,
    USE_SPECIAL_INTENTS,

    SHARD_COUNT,
    SHARD_INDEX,

    DEBUG_WEBHOOK,
    STATUS_WEBHOOK,

    PROFILE;

    private var value: String? = null

    companion object {
        fun init(properties: Properties) {
            values().forEach {
                try {
                    it.value = properties.getProperty(it.name)
                } catch (npe: NullPointerException) {
                    throw IllegalStateException("Settings not valid! Check console for more information", npe)
                }
            }
        }
    }

    fun get() = this.value!!
}
