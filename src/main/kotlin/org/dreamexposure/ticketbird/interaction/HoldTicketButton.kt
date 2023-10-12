package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.InteractionService
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class HoldTicketButton(
    private val interactionService: InteractionService,
): InteractionHandler<ButtonInteractionEvent> {
    override val ids = arrayOf("hold-ticket")

    override suspend fun handle(event: ButtonInteractionEvent, settings: GuildSettings) {
        event.deferReply()
                .withEphemeral(true)
                .awaitSingleOrNull()

        interactionService.holdTicketViaInteraction(true, event, settings)
    }
}
