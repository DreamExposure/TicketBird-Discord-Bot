package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent
import discord4j.core.`object`.entity.Message
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.TicketCreateStateCache
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.ProjectService
import org.dreamexposure.ticketbird.business.TicketService
import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.extensions.asSeconds
import org.dreamexposure.ticketbird.extensions.discord4j.deleteFollowupDelayed
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class SelectProjectWithCreateDropdown(
    private val ticketCreateStateCache: TicketCreateStateCache,
    private val ticketService: TicketService,
    private val projectService: ProjectService,
    private val localeService: LocaleService,
) : InteractionHandler<SelectMenuInteractionEvent> {
    override val ids = arrayOf("select-project-with-create")

    private val messageDeleteSeconds = Config.TIMING_MESSAGE_DELETE_TICKET_FLOW_SECONDS.getLong().asSeconds()

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
        event.createFollowup(localeService.getString(settings.locale, "generic.success.ticket-open", ticket.channel.asString()))
            .withEphemeral(true)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }
}
