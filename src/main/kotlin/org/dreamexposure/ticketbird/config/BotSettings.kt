package org.dreamexposure.ticketbird.config

import java.util.*

enum class BotSettings {
    LOG_FOLDER,
    USE_WEBHOOKS,

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
