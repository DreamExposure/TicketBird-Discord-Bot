package org.dreamexposure.ticketbird.extensions

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent
import kotlinx.coroutines.reactor.awaitSingle
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.ProjectService
import org.dreamexposure.ticketbird.business.TicketService
import org.dreamexposure.ticketbird.business.cache.CacheRepository
import org.dreamexposure.ticketbird.interaction.InteractionHandler
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.`object`.TicketCreateState
import org.springframework.stereotype.Component

@Component
class SelectProjectWithCreateDropdown(
    private val ticketCreateStateCache: CacheRepository<Long, TicketCreateState>,
    private val ticketService: TicketService,
    private val projectService: ProjectService,
    private val localeService: LocaleService,
): InteractionHandler<SelectMenuInteractionEvent> {
    override val id = "select-project-with-create"

    override suspend fun handle(event: SelectMenuInteractionEvent, settings: GuildSettings) {
        val selected = event.values[0]

        // Create ticket
        val ticket = ticketService.createNewTicketFull(
            guildId = settings.guildId,
            creatorId = event.interaction.user.id,
            project = projectService.getProject(settings.guildId, selected),
            info = ticketCreateStateCache.get(event.interaction.user.id.asLong())?.ticketInfo
        )

        // Respond
        event.createFollowup(localeService.getString(
            settings.locale,
            "modal.ticket-detail.response.success", //TODO: Swap out this string...
            ticket.channel.asString()
        )).awaitSingle()
    }
}
