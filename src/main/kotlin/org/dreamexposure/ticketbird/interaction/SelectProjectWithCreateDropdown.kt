package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent
import org.dreamexposure.ticketbird.TicketCreateStateCache
import org.dreamexposure.ticketbird.business.InteractionService
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class SelectProjectWithCreateDropdown(
    private val ticketCreateStateCache: TicketCreateStateCache,
    private val interactionService: InteractionService,
) : InteractionHandler<SelectMenuInteractionEvent> {
    override val ids = arrayOf("select-project-with-create")
    override val ephemeral = true

    override suspend fun handle(event: SelectMenuInteractionEvent, settings: GuildSettings) {
        val selected = event.values[0].toLong()
        val info = ticketCreateStateCache.getAndRemove(settings.guildId, event.interaction.user.id)?.ticketInfo.orEmpty()

        interactionService.openTicketViaInteraction(info, selected, ephemeral, event, settings)
    }
}
