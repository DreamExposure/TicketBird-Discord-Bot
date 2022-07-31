package org.dreamexposure.ticketbird.business.cache

import java.time.Duration

interface CacheService<K, V> {
    val ttl: Duration
        get() = Duration.ofMinutes(60)

    suspend fun put(key: K, value: V)

    suspend fun get(key: K): V?
}
