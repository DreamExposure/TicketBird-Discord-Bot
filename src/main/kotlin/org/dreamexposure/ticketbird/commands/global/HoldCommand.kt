package org.dreamexposure.ticketbird.commands.global

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import org.dreamexposure.ticketbird.business.InteractionService
import org.dreamexposure.ticketbird.commands.SlashCommand
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class HoldCommand(
    private val interactionService: InteractionService,
) : SlashCommand {
    override val name = "hold"
    override val hasSubcommands = false
    override val ephemeral = true

    override suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings) {
        interactionService.holdTicketViaInteraction(ephemeral, event, settings)
    }
}
