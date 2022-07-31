package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.database.GuildSettingsData
import org.dreamexposure.ticketbird.database.GuildSettingsRepository
import org.dreamexposure.ticketbird.extensions.asStringList
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.data.redis.cache.RedisCache
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.stereotype.Component

@Component
class DefaultGuildSettingsService(
    private val settingsRepository: GuildSettingsRepository,
    cacheManager: RedisCacheManager,
) : GuildSettingsService {
    private val cache = cacheManager.getCache("settingsCache")!! as RedisCache

    override suspend fun hasGuildSettings(guildId: Snowflake): Boolean {
        return settingsRepository.findByGuildId(guildId.asLong())
            .map { true }
            .awaitFirstOrDefault(false)
    }

    override suspend fun getGuildSettings(guildId: Snowflake): GuildSettings {
        val exists = cache.get(guildId.asLong(), GuildSettings::class.java)
        if (exists != null) return exists

        return settingsRepository.findByGuildId(guildId.asLong())
            .map(::GuildSettings)
            .defaultIfEmpty(GuildSettings(guildId = guildId))
            .doOnNext { cache.put(it.guildId.asLong(), it) }
            .awaitSingle()
    }

    @CachePut("settingsCache", key = "#settings.guildId.asLong()")
    override suspend fun createGuildSettings(settings: GuildSettings): GuildSettings {
        return settingsRepository.save(GuildSettingsData(
            guildId = settings.guildId.asLong(),
            lang = settings.locale.toLanguageTag(),
            devGuild = settings.devGuild,
            patronGuild = settings.patronGuild,
            useProjects = settings.useProjects,

            awaitingCategory = settings.awaitingCategory?.asLong(),
            respondedCategory = settings.respondedCategory?.asLong(),
            holdCategory = settings.holdCategory?.asLong(),
            closeCategory = settings.closeCategory?.asLong(),
            supportChannel = settings.supportChannel?.asLong(),
            staticMessage = settings.staticMessage?.asLong(),

            nextId = settings.nextId,
            staff = settings.staff.asStringList()
        )).map(::GuildSettings).awaitSingle()
    }

    @CachePut("settingsCache", key = "#settings.guildId.asLong()", condition = "#result != null")
    override suspend fun updateGuildSettings(settings: GuildSettings) {
        settingsRepository.updateByGuildId(
            guildId = settings.guildId.asLong(),
            lang = settings.locale.toLanguageTag(),
            devGuild = settings.devGuild,
            patronGuild = settings.patronGuild,
            useProjects = settings.useProjects,

            awaitingCategory = settings.awaitingCategory?.asLong(),
            respondedCategory = settings.respondedCategory?.asLong(),
            holdCategory = settings.holdCategory?.asLong(),
            closeCategory = settings.closeCategory?.asLong(),
            supportChannel = settings.supportChannel?.asLong(),
            staticMessage = settings.staticMessage?.asLong(),

            nextId = settings.nextId,
            staff = settings.staff.asStringList()
        ).awaitSingleOrNull()
    }

    @CacheEvict("settingsCache", key = "#guildId.asLong()")
    override suspend fun deleteGuildSettings(guildId: Snowflake) {
        settingsRepository.deleteByGuildId(guildId.asLong()).awaitSingle()
    }

    override suspend fun createOrUpdateGuildSettings(settings: GuildSettings): GuildSettings {
        return if (hasGuildSettings(settings.guildId)) {
            updateGuildSettings(settings)
            settings
        } else createGuildSettings(settings)
    }
}
