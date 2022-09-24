package org.dreamexposure.ticketbird.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.dreamexposure.ticketbird.GuildSettingsCache
import org.dreamexposure.ticketbird.ProjectCache
import org.dreamexposure.ticketbird.TicketCache
import org.dreamexposure.ticketbird.TicketCreateStateCache
import org.dreamexposure.ticketbird.business.cache.JdkCacheRepository
import org.dreamexposure.ticketbird.business.cache.RedisCacheRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import java.time.Duration

@Configuration
class CacheConfig(
    @Value("\${bot.cache.prefix}")
    private val prefix: String,
    @Value("\${bot.cache.ttl-minutes.settings:60}")
    private val settingsTtl: Long,
    @Value("\${bot.cache.ttl-minutes.ticket:60}")
    private val ticketTtl: Long,
    @Value("\${bot.cache.ttl-minutes.project:120}")
    private val projectTtl: Long,
    @Value("\${bot.cache.ttl-minutes.ticket-create-state:15}")
    private val ticketCreateStateTtl: Long,
) {
    // Cache name constants
    private val settingsCacheName = "$prefix.settingsCache"
    private val ticketCacheName = "$prefix.ticketCache"
    private val projectCacheName = "$prefix.projectCache"
    private val ticketCreateStateCacheName = "$prefix.ticketCreateStateCache"


    // Redis caching
    @Bean
    @ConditionalOnProperty("bot.cache.redis", havingValue = "true")
    fun redisCache(connection: RedisConnectionFactory): RedisCacheManager {
        return RedisCacheManager.builder(connection)
            .withCacheConfiguration(settingsCacheName,
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(settingsTtl))
            ).withCacheConfiguration(ticketCacheName,
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(ticketTtl))
            ).withCacheConfiguration(projectCacheName,
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(projectTtl))
            ).withCacheConfiguration(ticketCreateStateCacheName,
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(ticketCreateStateTtl)))
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
    fun guidSettingsFallbackCache(): GuildSettingsCache = JdkCacheRepository(Duration.ofMinutes(settingsTtl))

    @Bean
    fun ticketFallbackCache(): TicketCache = JdkCacheRepository(Duration.ofMinutes(ticketTtl))

    @Bean
    fun projectFallbackCache(): ProjectCache = JdkCacheRepository(Duration.ofMinutes(projectTtl))

    @Bean
    fun ticketCreateStateFallbackCache(): TicketCreateStateCache = JdkCacheRepository(Duration.ofMinutes(ticketCreateStateTtl))
}
