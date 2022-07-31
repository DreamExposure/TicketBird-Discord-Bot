package org.dreamexposure.ticketbird.business.cache

import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class FallbackCacheService<K, V> : CacheService<K, V> {
    private val cache = ConcurrentHashMap<K, Pair<Instant, V>>()

    init {
        Flux.interval(Duration.ofMinutes(5))
            .map { evict() }
            .subscribe()
    }

    override suspend fun put(key: K, value: V) {

        cache[key] = Pair(Instant.now(), value)
    }

    override suspend fun get(key: K): V? {
        return cache[key]?.second
    }

    private fun evict() {
        cache.forEach { (key, pair) -> if (Duration.between(pair.first, Instant.now()) > ttl) cache.remove(key) }
    }
}
