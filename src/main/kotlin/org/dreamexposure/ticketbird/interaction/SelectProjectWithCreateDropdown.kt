package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent
import kotlinx.coroutines.reactor.awaitSingleOrNull
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
    override suspend fun handle(event: SelectMenuInteractionEvent, settings: GuildSettings) {
        // Defer, it could take a moment
        event.deferReply().withEphemeral(true).awaitSingleOrNull()

        val selected = event.values[0].toLong()
        val info = ticketCreateStateCache.getAndRemove(settings.guildId, event.interaction.user.id)?.ticketInfo.orEmpty()

        interactionService.openTicketViaInteraction(info, selected, ephemeral = true, event, settings)
    }
}
