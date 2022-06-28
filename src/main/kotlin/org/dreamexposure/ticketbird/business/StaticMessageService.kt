package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Message
import discord4j.core.spec.EmbedCreateSpec

interface StaticMessageService {

    suspend fun getEmbed(guildId: Snowflake): EmbedCreateSpec?

    suspend fun update(guildId: Snowflake): Message?
}
