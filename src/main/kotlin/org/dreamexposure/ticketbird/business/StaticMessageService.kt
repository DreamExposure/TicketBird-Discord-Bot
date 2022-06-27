package org.dreamexposure.ticketbird.business

import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.spec.EmbedCreateSpec
import org.dreamexposure.ticketbird.`object`.GuildSettings

interface StaticMessageService {

    suspend fun getEmbed(guild: Guild, settings: GuildSettings): EmbedCreateSpec?

    suspend fun update(guild: Guild, settings: GuildSettings): Message?
}
