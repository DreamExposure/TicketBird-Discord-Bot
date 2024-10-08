package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent
import discord4j.core.`object`.component.TextInput
import org.dreamexposure.ticketbird.TicketCreateStateCache
import org.dreamexposure.ticketbird.business.InteractionService
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class TicketDetailModal(
    private val interactionService: InteractionService,
    private val ticketCreateStateCache: TicketCreateStateCache,
) : InteractionHandler<ModalSubmitInteractionEvent> {
    override val ids = arrayOf("ticket-detail")
    override val ephemeral = true

    override suspend fun handle(event: ModalSubmitInteractionEvent, settings: GuildSettings) {
        val projectId = ticketCreateStateCache.getAndRemove(settings.guildId, event.interaction.user.id)?.projectId ?: -1
        val info = event.getComponents(TextInput::class.java)[0].value.orElse(null)

        interactionService.openTicketViaInteraction(info, projectId, ephemeral, event, settings)
    }
}
