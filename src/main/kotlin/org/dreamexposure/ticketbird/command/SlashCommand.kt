package org.dreamexposure.ticketbird.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import org.dreamexposure.ticketbird.`object`.GuildSettings

interface SlashCommand {
    val name: String

    val ephemeral: Boolean

    suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings)
}
