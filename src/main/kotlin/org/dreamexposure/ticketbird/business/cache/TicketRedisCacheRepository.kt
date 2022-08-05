package org.dreamexposure.ticketbird.business.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.dreamexposure.ticketbird.`object`.Ticket
import org.springframework.cache.Cache
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.stereotype.Component

@Primary
@Component
class TicketRedisCacheRepository(
    redisCacheManager: RedisCacheManager,
    private val mapper: ObjectMapper,
): CacheRepository<Long, List<Ticket>> {
    private val cache: Cache = redisCacheManager.getCache("ticketRepository")!!

    override suspend fun put(key: Long, value: List<Ticket>) {
        mapper.writer()
        cache.put(key, mapper.writeValueAsString(value))
    }

    override suspend fun get(key: Long): List<Ticket>? {
        val raw = cache.get(key, String::class.java)
        return if (raw != null) mapper.readValue<List<Ticket>>(raw) else null
    }

    override suspend fun evict(key: Long) {
        cache.evictIfPresent(key)
    }
}

