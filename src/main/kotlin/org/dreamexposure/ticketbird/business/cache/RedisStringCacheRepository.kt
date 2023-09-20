package org.dreamexposure.ticketbird.business.cache

import com.fasterxml.jackson.databind.ObjectMapper
import discord4j.common.util.Snowflake
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.config.Config
import org.springframework.data.redis.connection.DataType
import org.springframework.data.redis.core.*
import java.time.Duration

class RedisStringCacheRepository<K, V>(
    private val valueType: Class<V>,
    private val objectMapper: ObjectMapper,
    private val redisTemplate: ReactiveStringRedisTemplate,
    override val ttl: Duration,
    cacheName: String,
) : CacheRepository<K, V> {
    private val valueOps = redisTemplate.opsForValue()
    private val keyPrefix = "${Config.CACHE_PREFIX.getString()}:$cacheName"

    init {
        objectMapper.writer()
    }

    override suspend fun put(guildId: Snowflake?, key: K, value: V) {
        valueOps.setAndAwait(formatKey(guildId, key), objectMapper.writeValueAsString(value), ttl)
    }

    override suspend fun putMany(guildId: Snowflake?, values: Map<K, V>) {
        values.forEach { (key, value) ->
            valueOps.setAndAwait(formatKey(guildId, key), objectMapper.writeValueAsString(value), ttl)
        }
    }


    override suspend fun get(guildId: Snowflake?, key: K): V? {
        val raw = valueOps.getAndAwait(formatKey(guildId, key))
        return if (raw != null) objectMapper.readValue(raw, valueType) else null
    }

    override suspend fun getAll(guildId: Snowflake?): List<V> {
        val keys = redisTemplate.scan(ScanOptions.scanOptions()
            .type(DataType.STRING)
            .match(formatKeySearch(guildId))
            .build()
        ).collectList().awaitSingle()

        return valueOps.multiGetAndAwait(keys).map { objectMapper.readValue(it, valueType) }
    }


    override suspend fun getAndRemove(guildId: Snowflake?, key: K): V? {
        val raw = valueOps.getAndDelete(formatKey(guildId, key)).awaitSingleOrNull()
        return if (raw != null) objectMapper.readValue(raw, valueType) else null
    }

    override suspend fun getAndRemoveAll(guildId: Snowflake?): List<V> {
        return redisTemplate.scan(ScanOptions.scanOptions().type(DataType.STRING).match(formatKeySearch(guildId)).build())
            .flatMap(valueOps::getAndDelete)
            .map { objectMapper.readValue(it, valueType) }
            .collectList()
            .awaitSingle()
    }


    override suspend fun evict(guildId: Snowflake?, key: K) {
        valueOps.deleteAndAwait(formatKey(guildId, key))
    }

    override suspend fun evictAll(guildId: Snowflake?) {
        val keys = redisTemplate.scan(ScanOptions.scanOptions()
            .type(DataType.STRING)
            .match(formatKeySearch(guildId))
            .build()
        ).collectList().awaitSingle()

        redisTemplate.deleteAndAwait(*keys.toTypedArray())
    }


    private fun formatKey(guildId: Snowflake?, key: K): String {
        val normalizedGuildId = guildId?.asString() ?: "_"
        val normalizedKey = objectMapper.writeValueAsString(key)

        return "$keyPrefix:$normalizedGuildId:$normalizedKey"
    }

    private fun formatKeySearch(guildId: Snowflake?): String {
        val normalizedGuildId = guildId?.asString() ?: "*"
        return "$keyPrefix:$normalizedGuildId:*"
    }


    companion object {
        inline operator fun <K : Any, reified V> invoke(
            objectMapper: ObjectMapper,
            redisTemplate: ReactiveStringRedisTemplate,
            cacheName: String,
            ttl: Duration,
        ) = RedisStringCacheRepository<K, V>(V::class.java, objectMapper, redisTemplate, ttl, cacheName)
    }
}
