package org.dreamexposure.ticketbird.business.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.cache.Cache
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.stereotype.Component

@Primary
@Component
class GuildSettingsRedisCacheRepository(
    redisCacheManager: RedisCacheManager,
    private val mapper: ObjectMapper,
): CacheRepository<Long, GuildSettings> {
    private val cache: Cache = redisCacheManager.getCache("settingsRepository")!!

    override suspend fun put(key: Long, value: GuildSettings) {
        mapper.writer()
        cache.put(key, mapper.writeValueAsString(value))
    }

    override suspend fun get(key: Long): GuildSettings? {
        val raw = cache.get(key, String::class.java)
        return if (raw != null) mapper.readValue<GuildSettings>(raw) else null
    }

    override suspend fun evict(key: Long) {
        cache.evictIfPresent(key)
    }
}
