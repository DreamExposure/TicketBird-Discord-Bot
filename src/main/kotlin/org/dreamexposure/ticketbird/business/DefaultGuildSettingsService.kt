package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.GuildSettingsCache
import org.dreamexposure.ticketbird.database.GuildSettingsData
import org.dreamexposure.ticketbird.database.GuildSettingsRepository
import org.dreamexposure.ticketbird.extensions.asStringList
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class DefaultGuildSettingsService(
    private val settingsRepository: GuildSettingsRepository,
    private val settingsCache: GuildSettingsCache
) : GuildSettingsService {

    override suspend fun hasGuildSettings(guildId: Snowflake): Boolean {
        return settingsRepository.findByGuildId(guildId.asLong()).hasElement().awaitSingle()
    }

    override suspend fun getGuildSettings(guildId: Snowflake): GuildSettings {
        var settings = settingsCache.get(guildId.asLong())
        if (settings != null) return settings

        settings = settingsRepository.findByGuildId(guildId.asLong())
            .map(::GuildSettings)
            .defaultIfEmpty(GuildSettings(guildId = guildId))
            .awaitSingle()

        settingsCache.put(guildId.asLong(), settings)
        return settings
    }

    override suspend fun createGuildSettings(settings: GuildSettings): GuildSettings {
        val saved =  settingsRepository.save(GuildSettingsData(
            guildId = settings.guildId.asLong(),
            lang = settings.locale.toLanguageTag(),
            devGuild = settings.devGuild,
            patronGuild = settings.patronGuild,
            useProjects = settings.useProjects,
            autoCloseHours = settings.autoClose.toHours().toInt(),
            autoDeleteHours = settings.autoDelete.toHours().toInt(),
            requiresRepair = settings.requiresRepair,

            awaitingCategory = settings.awaitingCategory?.asLong(),
            respondedCategory = settings.respondedCategory?.asLong(),
            holdCategory = settings.holdCategory?.asLong(),
            closeCategory = settings.closeCategory?.asLong(),
            supportChannel = settings.supportChannel?.asLong(),
            staticMessage = settings.staticMessage?.asLong(),

            nextId = settings.nextId,
            staff = settings.staff.asStringList(),
            staffRole = settings.staffRole?.asLong(),
            pingOption = settings.pingOption.value,
        )).map(::GuildSettings).awaitSingle()

        settingsCache.put(settings.guildId.asLong(), saved)
        return saved
    }

    override suspend fun updateGuildSettings(settings: GuildSettings) {
        settingsRepository.updateByGuildId(
            guildId = settings.guildId.asLong(),
            lang = settings.locale.toLanguageTag(),
            devGuild = settings.devGuild,
            patronGuild = settings.patronGuild,
            useProjects = settings.useProjects,
            autoCloseHours = settings.autoClose.toHours().toInt(),
            autoDeleteHours = settings.autoDelete.toHours().toInt(),
            requiresRepair = settings.requiresRepair,

            awaitingCategory = settings.awaitingCategory?.asLong(),
            respondedCategory = settings.respondedCategory?.asLong(),
            holdCategory = settings.holdCategory?.asLong(),
            closeCategory = settings.closeCategory?.asLong(),
            supportChannel = settings.supportChannel?.asLong(),
            staticMessage = settings.staticMessage?.asLong(),

            nextId = settings.nextId,
            staff = settings.staff.asStringList(),
            staffRole = settings.staffRole?.asLong(),
            pingOption = settings.pingOption.value,
        ).awaitSingleOrNull()

        settingsCache.put(settings.guildId.asLong(), settings)
    }

    override suspend fun deleteGuildSettings(guildId: Snowflake) {
        settingsRepository.deleteByGuildId(guildId.asLong()).awaitSingle()
        settingsCache.evict(guildId.asLong())
    }
}
