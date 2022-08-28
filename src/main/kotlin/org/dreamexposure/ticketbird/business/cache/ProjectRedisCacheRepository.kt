package org.dreamexposure.ticketbird.business.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.dreamexposure.ticketbird.`object`.Project
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.Cache
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.stereotype.Component

@Primary
@Component
@ConditionalOnProperty("bot.cache.redis", havingValue = "true")
class ProjectRedisCacheRepository(
    @Value("\${bot.cache.prefix}") prefix: String,
    redisCacheManager: RedisCacheManager,
    private val mapper: ObjectMapper,
): CacheRepository<Long, List<Project>> {
    private val cache: Cache = redisCacheManager.getCache("$prefix.projectRepository")!!

    override suspend fun put(key: Long, value: List<Project>) {
        mapper.writer()
        cache.put(key, mapper.writeValueAsString(value))
    }

    override suspend fun get(key: Long): List<Project>? {
        val raw = cache.get(key, String::class.java)
        return if (raw != null) mapper.readValue<List<Project>>(raw) else null
    }

    override suspend fun getAndRemove(key: Long): List<Project>? {
        val raw = cache.get(key, String::class.java)
        val parsed =  if (raw != null) mapper.readValue<List<Project>>(raw) else null

        evict(key)
        return parsed
    }

    override suspend fun evict(key: Long) {
        cache.evictIfPresent(key)
    }
}
