package org.dreamexposure.ticketbird.config

import java.io.FileReader
import java.util.*

enum class Config(private val key: String, private var value: Any? = null) {
    // Basic spring settings
    APP_NAME("spring.application.name"),

    // Redis cache settings
    REDIS_HOST("spring.data.redis.host"),
    REDIS_PASSWORD("spring.data.redis.password", ""),
    REDIS_PORT("spring.data.redis.port"),
    CACHE_REDIS_IS_CLUSTER("redis.cluster", false),
    CACHE_USE_REDIS("bot.cache.redis", false),
    CACHE_PREFIX("bot.cache.prefix", "ticketbird"),
    CACHE_TTL_SETTINGS_MINUTES("bot.cache.ttl-minutes.settings", 60),
    CACHE_TTL_TICKET_MINUTES("bot.cache.ttl-minutes.ticket", 60),
    CACHE_TTL_PROJECT_MINUTES("bot.cache.ttl-minutes.project", 120),
    CACHE_TTL_TICKET_CREATE_STATE_MINUTES("bot.cache.ttl-minutes.ticket-create-state", 15),

    // Global bot timings
    TIMING_ACTIVITY_MONITOR_FREQUENCY_MINUTES("bot.timing.activity-monitor.frequency.minutes", 60),
    TIMING_MESSAGE_DELETE_TICKET_FLOW_SECONDS("bot.timing.message-delete.open-ticket-flow.seconds", 60),
    TIMING_MESSAGE_DELETE_GENERIC_SECONDS("bot, timing.message-delete.generic.seconds", 30),

    // Bot secrets
    SECRET_BOT_TOKEN("bot.secret.token"),
    SECRET_CLIENT_SECRET("bot.secret.client-secret"),
    SECRET_WEBHOOK_DEBUG("bot.secret.debug-webhook"),
    SECRET_WEBHOOK_STATUS("bot.secret.status-webhook"),

    // Various URLs
    URL_BASE("bot.url.base"),
    URL_SUPPORT("bot.url.support", "https://discord.gg/2TFqyuy"),
    URL_INVITE("bot.url.invite"),


    // Everything else
    SHARD_COUNT("bot.sharding.count"),
    SHARD_INDEX("bot.sharding.index"),
    LOGGING_WEBHOOKS_USE("bot.logging.webhooks.use", false),
    LOGGING_WEBHOOKS_ALL_ERRORS("bot.logging.webhooks.all-errors", false),
    // TODO: Remove toggle when no longer needed
    TOGGLE_TICKET_LOGGING("bot.feature.toggle.ticket-logging", false),
    ;

    companion object {
        fun init() {
            val props = Properties()
            props.load(FileReader("application.properties"))

            entries.forEach { it.value = props.getProperty(it.key, it.value?.toString()) }
        }
    }


    fun getString() = value.toString()

    fun getInt() = getString().toInt()

    fun getLong() = getString().toLong()

    fun getBoolean() = getString().toBoolean()
}
