package org.dreamexposure.ticketbird.commands

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import org.dreamexposure.ticketbird.`object`.GuildSettings

interface SlashCommand {
    val name: String
    val hasSubcommands: Boolean
    val ephemeral: Boolean

    suspend fun shouldDefer(event: ChatInputInteractionEvent): Boolean = true

    suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings)
}
