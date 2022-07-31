package org.dreamexposure.ticketbird.config

import java.util.*

enum class BotSettings {
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

    SHARD_COUNT,
    SHARD_INDEX,

    DEBUG_WEBHOOK,
    STATUS_WEBHOOK,

    BASE_URL,
    SUPPORT_URL,
    INVITE_URL,

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
