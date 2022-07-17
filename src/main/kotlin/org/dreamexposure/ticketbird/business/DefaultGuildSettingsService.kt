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
        return settingsRepository.findByGuildId(guildId.asLong())
            .map { true }
            .awaitFirstOrDefault(false)
    }

    override suspend fun getGuildSettings(guildId: Snowflake): GuildSettings {
        return settingsRepository.findByGuildId(guildId.asLong())
            .map(::GuildSettings)
            .awaitFirstOrDefault(GuildSettings(guildId = guildId))
    }

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
