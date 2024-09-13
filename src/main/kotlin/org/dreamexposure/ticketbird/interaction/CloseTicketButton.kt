package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import org.dreamexposure.ticketbird.business.InteractionService
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class CloseTicketButton(
    private val interactionService: InteractionService,
): InteractionHandler<ButtonInteractionEvent> {
    override val ids = arrayOf("close-ticket")
    override val ephemeral = true

    override suspend fun handle(event: ButtonInteractionEvent, settings: GuildSettings) {
        interactionService.closeTicketViaInteraction(ephemeral, event, settings)
    }
}
