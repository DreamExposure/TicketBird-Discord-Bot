package org.dreamexposure.ticketbird.business.cache

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.cache.RedisCacheManager

class RedisCacheService<K, V>(
    private val cacheManager: RedisCacheManager,
    private val objectMapper: ObjectMapper,
    val cacheName: String,
): CacheService<K, V> {
    private val cache = cacheManager.getCache(cacheName)!!

    override suspend fun put(key: K, value: V) {
        val mappedValue = objectMapper.writeValueAsString(value)

        cache.put(key as Any, mappedValue)

        TODO("Not yet implemented")
    }

    override suspend fun get(key: K): V? {
        val cached = cache.get(key as Any, String::class.java)
        if (cached == null) return null



        TODO("Not yet implemented")
    }
}
