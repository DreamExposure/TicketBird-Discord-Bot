package org.dreamexposure.ticketbird.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.dreamexposure.ticketbird.GuildSettingsCache
import org.dreamexposure.ticketbird.ProjectCache
import org.dreamexposure.ticketbird.TicketCache
import org.dreamexposure.ticketbird.TicketCreateStateCache
import org.dreamexposure.ticketbird.business.cache.JdkCacheRepository
import org.dreamexposure.ticketbird.business.cache.RedisStringCacheRepository
import org.dreamexposure.ticketbird.extensions.asMinutes
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.core.ReactiveStringRedisTemplate

@Configuration
class CacheConfig {
    private val settingsTtl = Config.CACHE_TTL_SETTINGS_MINUTES.getLong().asMinutes()
    private val ticketTtl = Config.CACHE_TTL_TICKET_MINUTES.getLong().asMinutes()
    private val projectTtl = Config.CACHE_TTL_PROJECT_MINUTES.getLong().asMinutes()
    private val ticketCreateStateTtl = Config.CACHE_TTL_TICKET_CREATE_STATE_MINUTES.getLong().asMinutes()


    // Redis caching
    @Bean
    @Primary
    @ConditionalOnProperty("bot.cache.redis", havingValue = "true")
    fun guildSettingsRedisCache(objectMapper: ObjectMapper, redisTemplate: ReactiveStringRedisTemplate): GuildSettingsCache =
        RedisStringCacheRepository(objectMapper, redisTemplate, "GuildSettings", settingsTtl)

    @Bean
    @Primary
    @ConditionalOnProperty("bot.cache.redis", havingValue = "true")
    fun ticketRedisCache(objectMapper: ObjectMapper, redisTemplate: ReactiveStringRedisTemplate): TicketCache =
        RedisStringCacheRepository(objectMapper, redisTemplate, "Tickets", ticketTtl)

    @Bean
    @Primary
    @ConditionalOnProperty("bot.cache.redis", havingValue = "true")
    fun projectRedisCache(objectMapper: ObjectMapper, redisTemplate: ReactiveStringRedisTemplate): ProjectCache =
        RedisStringCacheRepository(objectMapper, redisTemplate, "Projects", projectTtl)

    @Bean
    @Primary
    @ConditionalOnProperty("bot.cache.redis", havingValue = "true")
    fun ticketCreateStateRedisCache(objectMapper: ObjectMapper, redisTemplate: ReactiveStringRedisTemplate): TicketCreateStateCache =
        RedisStringCacheRepository(objectMapper, redisTemplate, "TicketCreateStates", ticketCreateStateTtl)


    // In-memory fallback caching
    @Bean
    fun guidSettingsFallbackCache(): GuildSettingsCache = JdkCacheRepository(settingsTtl)

    @Bean
    fun ticketFallbackCache(): TicketCache = JdkCacheRepository(ticketTtl)

    @Bean
    fun projectFallbackCache(): ProjectCache = JdkCacheRepository(projectTtl)

    @Bean
    fun ticketCreateStateFallbackCache(): TicketCreateStateCache = JdkCacheRepository(ticketCreateStateTtl)
}
