package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.database.GuildSettingsData
import org.dreamexposure.ticketbird.database.GuildSettingsRepository
import org.dreamexposure.ticketbird.extensions.asStringList
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class DefaultGuildSettingsService(private val settingsRepository: GuildSettingsRepository) : GuildSettingsService {
    override suspend fun hasGuildSettings(guildId: Snowflake): Boolean {
        return settingsRepository.existsById(guildId.asLong()).awaitSingle()
    }

    override suspend fun getGuildSettings(guildId: Snowflake): GuildSettings {
        return settingsRepository.findById(guildId.asLong())
            .map(::GuildSettings)
            .awaitFirstOrDefault(GuildSettings(guildId = guildId))
    }

    override suspend fun createGuildSettings(settings: GuildSettings): GuildSettings {
        return settingsRepository.save(GuildSettingsData(
            guildId = settings.guildId.asLong(),
            lang = settings.lang,
            prefix = settings.prefix,
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
            closedTotal = settings.totalClosed,
            staff = settings.staff.asStringList()
        )).map(::GuildSettings).awaitSingle()
    }

    override suspend fun updateGuildSettings(settings: GuildSettings) {
        settingsRepository.updateById(
            guildId = settings.guildId.asLong(),
            lang = settings.lang,
            prefix = settings.prefix,
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
            closedTotal = settings.totalClosed,
            staff = settings.staff.asStringList()
        ).awaitSingleOrNull()
    }

    override suspend fun deleteGuildSettings(guildId: Snowflake) {
        settingsRepository.deleteById(guildId.asLong()).awaitSingle()
    }

    override suspend fun createOrUpdateGuildSettings(settings: GuildSettings): GuildSettings {
        return if (hasGuildSettings(settings.guildId)) {
            updateGuildSettings(settings)
            settings
        } else createGuildSettings(settings)
    }
}
