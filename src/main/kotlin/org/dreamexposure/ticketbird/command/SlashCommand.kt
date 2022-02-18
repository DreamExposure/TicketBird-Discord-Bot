package org.dreamexposure.ticketbird.command

import discord4j.core.`object`.entity.Message
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import org.dreamexposure.ticketbird.`object`.GuildSettings
import reactor.core.publisher.Mono

interface SlashCommand {
    val name: String

    val ephemeral: Boolean

    fun handle(event: ChatInputInteractionEvent, settings: GuildSettings): Mono<Message>
}
