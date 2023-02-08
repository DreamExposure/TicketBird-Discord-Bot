package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import org.dreamexposure.ticketbird.`object`.GuildSettings

interface GuildSettingsService {
    suspend fun hasGuildSettings(guildId: Snowflake): Boolean

    suspend fun getGuildSettings(guildId: Snowflake): GuildSettings

    suspend fun createGuildSettings(settings: GuildSettings): GuildSettings

    suspend fun updateGuildSettings(settings: GuildSettings)

    suspend fun deleteGuildSettings(guildId: Snowflake)

    suspend fun upsertGuildSettings(settings: GuildSettings): GuildSettings {
        if (hasGuildSettings(settings.guildId)) updateGuildSettings(settings)
        else return createGuildSettings(settings)
        return settings
    }
}
