package org.dreamexposure.ticketbird.business.cache

import org.dreamexposure.ticketbird.extensions.isExpiredTtl
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class JdkCacheRepository<K : Any, V>(override val ttl: Duration) : CacheRepository<K, V> {
    private val cache = ConcurrentHashMap<K, Pair<Instant, V>>()

    init {
        Flux.interval(Duration.ofMinutes(5))
            .map { evictOld() }
            .subscribe()
    }

    override suspend fun put(key: K, value: V) {
        cache[key] = Pair(Instant.now().plus(ttl), value)
    }

    override suspend fun get(key: K): V? {
        val cached = cache[key] ?: return null
        if (cached.first.isExpiredTtl()) {
            evict(key)
            return null
        }


        return cached.second
    }

    override suspend fun getAndRemove(key: K): V? {
        val cached = cache[key] ?: return null
        evict(key)

        return if (cached.first.isExpiredTtl()) null else cached.second
    }

    override suspend fun evict(key: K) {
        cache.remove(key)
    }

    private fun evictOld() {
        cache.forEach { (key, pair) -> if (pair.first.isExpiredTtl()) cache.remove(key) }
    }
}
