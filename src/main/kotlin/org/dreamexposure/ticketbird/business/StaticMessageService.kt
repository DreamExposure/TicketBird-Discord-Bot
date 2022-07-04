package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import discord4j.core.`object`.component.LayoutComponent
import discord4j.core.`object`.entity.Message
import discord4j.core.spec.EmbedCreateSpec
import org.dreamexposure.ticketbird.`object`.GuildSettings

interface StaticMessageService {

    suspend fun getEmbed(settings: GuildSettings): EmbedCreateSpec?

    suspend fun getComponents(settings: GuildSettings): Array<LayoutComponent>

    suspend fun update(guildId: Snowflake): Message?
}
