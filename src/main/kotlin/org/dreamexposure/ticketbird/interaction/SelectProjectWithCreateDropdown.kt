package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.ProjectService
import org.dreamexposure.ticketbird.business.TicketService
import org.dreamexposure.ticketbird.business.cache.CacheRepository
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.`object`.TicketCreateState
import org.springframework.stereotype.Component

@Component
class SelectProjectWithCreateDropdown(
    private val ticketCreateStateCache: CacheRepository<String, TicketCreateState>,
    private val ticketService: TicketService,
    private val projectService: ProjectService,
    private val localeService: LocaleService,
): InteractionHandler<SelectMenuInteractionEvent> {
    override val ids = arrayOf("select-project-with-create")

    override suspend fun handle(event: SelectMenuInteractionEvent, settings: GuildSettings) {
        val selected = event.values[0]

        // Defer, it could take a moment
        event.deferReply().withEphemeral(true).awaitSingleOrNull()

        // Create ticket
        val ticket = ticketService.createNewTicketFull(
            guildId = settings.guildId,
            creatorId = event.interaction.user.id,
            project = projectService.getProject(settings.guildId, selected),
            info = ticketCreateStateCache.getAndRemove("${settings.guildId}.${event.interaction.user.id.asLong()}")?.ticketInfo
        )

        // Respond
        event.createFollowup(localeService.getString(
            settings.locale,
            "generic.success.ticket-open",
            ticket.channel.asString()
        )).awaitSingle()
    }
}
