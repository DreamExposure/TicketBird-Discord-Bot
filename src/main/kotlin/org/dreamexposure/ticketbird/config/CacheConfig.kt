package org.dreamexposure.ticketbird.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.dreamexposure.ticketbird.GuildSettingsCache
import org.dreamexposure.ticketbird.ProjectCache
import org.dreamexposure.ticketbird.TicketCache
import org.dreamexposure.ticketbird.TicketCreateStateCache
import org.dreamexposure.ticketbird.business.cache.JdkCacheRepository
import org.dreamexposure.ticketbird.business.cache.RedisCacheRepository
import org.dreamexposure.ticketbird.extensions.asMinutes
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory

@Configuration
class CacheConfig {
    // Cache name constants
    private val prefix = Config.CACHE_PREFIX.getString()
    private val settingsCacheName = "$prefix.settingsCache"
    private val ticketCacheName = "$prefix.ticketCache"
    private val projectCacheName = "$prefix.projectCache"
    private val ticketCreateStateCacheName = "$prefix.ticketCreateStateCache"

    private val settingsTtl = Config.CACHE_TTL_SETTINGS_MINUTES.getLong().asMinutes()
    private val ticketTtl = Config.CACHE_TTL_TICKET_MINUTES.getLong().asMinutes()
    private val projectTtl = Config.CACHE_TTL_PROJECT_MINUTES.getLong().asMinutes()
    private val ticketCreateStateTtl = Config.CACHE_TTL_TICKET_CREATE_STATE_MINUTES.getLong().asMinutes()


    // Redis caching
    @Bean
    @ConditionalOnProperty("bot.cache.redis", havingValue = "true")
    fun redisCache(connection: RedisConnectionFactory): RedisCacheManager {
        return RedisCacheManager.builder(connection)
            .withCacheConfiguration(settingsCacheName,
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(settingsTtl)
            ).withCacheConfiguration(ticketCacheName,
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(ticketTtl)
            ).withCacheConfiguration(projectCacheName,
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(projectTtl)
            ).withCacheConfiguration(ticketCreateStateCacheName,
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(ticketCreateStateTtl))
            .build()
    }

    @Bean
    @Primary
    @ConditionalOnProperty("bot.cache.redis", havingValue = "true")
    fun guildSettingsRedisCache(cacheManager: RedisCacheManager, objectMapper: ObjectMapper): GuildSettingsCache =
        RedisCacheRepository(cacheManager, objectMapper, settingsCacheName)

    @Bean
    @Primary
    @ConditionalOnProperty("bot.cache.redis", havingValue = "true")
    fun ticketRedisCache(cacheManager: RedisCacheManager, objectMapper: ObjectMapper): TicketCache =
        RedisCacheRepository(cacheManager, objectMapper, ticketCacheName)

    @Bean
    @Primary
    @ConditionalOnProperty("bot.cache.redis", havingValue = "true")
    fun projectRedisCache(cacheManager: RedisCacheManager, objectMapper: ObjectMapper): ProjectCache =
        RedisCacheRepository(cacheManager, objectMapper, projectCacheName)

    @Bean
    @Primary
    @ConditionalOnProperty("bot.cache.redis", havingValue = "true")
    fun ticketCreateStateRedisCache(cacheManager: RedisCacheManager, objectMapper: ObjectMapper): TicketCreateStateCache =
        RedisCacheRepository(cacheManager, objectMapper, ticketCreateStateCacheName)

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
