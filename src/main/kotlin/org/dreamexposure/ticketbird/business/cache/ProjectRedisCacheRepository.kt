package org.dreamexposure.ticketbird.business.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.dreamexposure.ticketbird.`object`.Project
import org.springframework.cache.Cache
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.stereotype.Component

@Primary
@Component
class ProjectRedisCacheRepository(
    redisCacheManager: RedisCacheManager,
    private val mapper: ObjectMapper,
): CacheRepository<Long, List<Project>> {
    private val cache: Cache = redisCacheManager.getCache("projectRepository")!!

    override suspend fun put(key: Long, value: List<Project>) {
        mapper.writer()
        cache.put(key, mapper.writeValueAsString(value))
    }

    override suspend fun get(key: Long): List<Project>? {
        val raw = cache.get(key, String::class.java)
        return if (raw != null) mapper.readValue<List<Project>>(raw) else null
    }

    override suspend fun evict(key: Long) {
        cache.evictIfPresent(key)
    }
}
