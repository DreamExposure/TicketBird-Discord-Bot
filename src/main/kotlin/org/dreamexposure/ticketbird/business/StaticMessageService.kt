package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Message
import discord4j.core.spec.EmbedCreateSpec
import org.dreamexposure.ticketbird.`object`.GuildSettings

interface StaticMessageService {

    suspend fun getEmbed(settings: GuildSettings): EmbedCreateSpec?

    suspend fun update(guildId: Snowflake): Message?
}
