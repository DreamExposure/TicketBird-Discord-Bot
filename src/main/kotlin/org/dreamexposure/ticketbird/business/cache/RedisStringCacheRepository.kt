package org.dreamexposure.ticketbird.business.cache

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.deleteAndAwait
import org.springframework.data.redis.core.getAndAwait
import org.springframework.data.redis.core.setAndAwait

class RedisStringCacheRepository<V>(
    private val valueType: Class<V>,
    private val objectMapper: ObjectMapper,
    redisTemplate: ReactiveStringRedisTemplate,
): CacheRepository<String, V> {
    private val valueOps = redisTemplate.opsForValue()

    init {
        objectMapper.writer()
    }

    override suspend fun put(key: String, value: V) {
        valueOps.setAndAwait(key, objectMapper.writeValueAsString(value), ttl)
    }

    override suspend fun get(key: String): V? {
        val raw = valueOps.getAndAwait(key)
        return if (raw != null) objectMapper.readValue(raw, valueType) else null
    }

    override suspend fun getAndRemove(key: String): V? {
        val raw = valueOps.getAndDelete(key).awaitSingleOrNull()
        return if (raw != null) objectMapper.readValue(raw, valueType) else null
    }

    override suspend fun evict(key: String) {
        valueOps.deleteAndAwait(key)
    }

    companion object {
        inline operator fun <reified V> invoke(
            objectMapper: ObjectMapper,
            redisTemplate: ReactiveStringRedisTemplate,
        ) = RedisStringCacheRepository(V::class.java, objectMapper, redisTemplate)
    }

}
