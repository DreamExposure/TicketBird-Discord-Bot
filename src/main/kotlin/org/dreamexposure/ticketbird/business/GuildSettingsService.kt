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
class GuildSettingsService(
    private val settingsRepository: GuildSettingsRepository,
    private val settingsCache: GuildSettingsCache
) {

    suspend fun hasGuildSettings(guildId: Snowflake): Boolean {
        return settingsRepository.findByGuildId(guildId.asLong()).hasElement().awaitSingle()
    }

    suspend fun getGuildSettings(guildId: Snowflake): GuildSettings {
        var settings = settingsCache.get(key = guildId)
        if (settings != null) return settings

        settings = settingsRepository.findByGuildId(guildId.asLong())
            .map(::GuildSettings)
            .defaultIfEmpty(GuildSettings(guildId = guildId))
            .awaitSingle()

        settingsCache.put(key = guildId, value = settings)
        return settings
    }

    suspend fun createGuildSettings(settings: GuildSettings): GuildSettings {
        val saved =  settingsRepository.save(GuildSettingsData(
            guildId = settings.guildId.asLong(),
            lang = settings.locale.toLanguageTag(),
            devGuild = settings.devGuild,
            patronGuild = settings.patronGuild,
            useProjects = settings.useProjects,
            enableLogging = settings.enableLogging,
            showTicketStats = settings.showTicketStats,
            autoCloseHours = settings.autoClose.toHours().toInt(),
            autoDeleteHours = settings.autoDelete.toHours().toInt(),
            requiresRepair = settings.requiresRepair,

            awaitingCategory = settings.awaitingCategory?.asLong(),
            respondedCategory = settings.respondedCategory?.asLong(),
            holdCategory = settings.holdCategory?.asLong(),
            closeCategory = settings.closeCategory?.asLong(),
            supportChannel = settings.supportChannel?.asLong(),
            logChannel = settings.logChannel?.asLong(),
            staticMessage = settings.staticMessage?.asLong(),

            nextId = settings.nextId,
            staff = settings.staff.asStringList(),
            staffRole = settings.staffRole?.asLong(),
            pingOption = settings.pingOption.value,
        )).map(::GuildSettings).awaitSingle()

        settingsCache.put(key = saved.guildId, value = saved)
        return saved
    }

    suspend fun updateGuildSettings(settings: GuildSettings) {
        settingsRepository.updateByGuildId(
            guildId = settings.guildId.asLong(),
            lang = settings.locale.toLanguageTag(),
            devGuild = settings.devGuild,
            patronGuild = settings.patronGuild,
            useProjects = settings.useProjects,
            enableLogging = settings.enableLogging,
            showTicketStats = settings.showTicketStats,
            autoCloseHours = settings.autoClose.toHours().toInt(),
            autoDeleteHours = settings.autoDelete.toHours().toInt(),
            requiresRepair = settings.requiresRepair,

            awaitingCategory = settings.awaitingCategory?.asLong(),
            respondedCategory = settings.respondedCategory?.asLong(),
            holdCategory = settings.holdCategory?.asLong(),
            closeCategory = settings.closeCategory?.asLong(),
            supportChannel = settings.supportChannel?.asLong(),
            logChannel = settings.logChannel?.asLong(),
            staticMessage = settings.staticMessage?.asLong(),

            nextId = settings.nextId,
            staff = settings.staff.asStringList(),
            staffRole = settings.staffRole?.asLong(),
            pingOption = settings.pingOption.value,
        ).awaitSingleOrNull()

        settingsCache.put(key = settings.guildId, value = settings)
    }

    suspend fun deleteGuildSettings(guildId: Snowflake) {
        settingsRepository.deleteByGuildId(guildId.asLong()).awaitSingle()
        settingsCache.evict(key = guildId)
    }

    suspend fun upsertGuildSettings(settings: GuildSettings): GuildSettings {
        if (hasGuildSettings(settings.guildId)) updateGuildSettings(settings)
        else return createGuildSettings(settings)
        return settings
    }
}
