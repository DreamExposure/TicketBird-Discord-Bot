package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent
import discord4j.core.`object`.component.TextInput
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
class TicketDetailModal(
    private val ticketService: TicketService,
    private val localeService: LocaleService,
    private val projectService: ProjectService,
    private val ticketCreateStateCache: TicketCreateStateCache,
) : InteractionHandler<ModalSubmitInteractionEvent> {
    override val ids = arrayOf("ticket-detail")

    private val messageDeleteSeconds = Config.TIMING_MESSAGE_DELETE_TICKET_FLOW_SECONDS.getLong().asSeconds()

    override suspend fun handle(event: ModalSubmitInteractionEvent, settings: GuildSettings) {
        // Defer, it could take a moment
        event.deferReply().withEphemeral(true).awaitSingleOrNull()

        val projectName = ticketCreateStateCache.getAndRemove("${settings.guildId}.${event.interaction.user.id.asLong()}")?.projectName.orEmpty()

        // Create ticket
        val ticket = ticketService.createNewTicketFull(
            guildId = settings.guildId,
            creatorId = event.interaction.user.id,
            project = projectService.getProject(settings.guildId, projectName),
            info = event.getComponents(TextInput::class.java)[0].value.orElse(null)
        )

        // Respond
        event.createFollowup(localeService.getString(settings.locale, "generic.success.ticket-open", ticket.channel.asString()))
            .withEphemeral(true)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }
}
