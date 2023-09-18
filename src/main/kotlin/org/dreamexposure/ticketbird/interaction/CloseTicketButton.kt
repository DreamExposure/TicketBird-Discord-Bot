package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.InteractionService
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class CloseTicketButton(
    private val interactionService: InteractionService,
): InteractionHandler<ButtonInteractionEvent> {
    override val ids = arrayOf("close-ticket")

    override suspend fun handle(event: ButtonInteractionEvent, settings: GuildSettings) {
    event.deferReply()
            .withEphemeral(true)
            .awaitSingleOrNull()

        interactionService.closeTicketViaInteraction(true, event, settings)
    }
}
