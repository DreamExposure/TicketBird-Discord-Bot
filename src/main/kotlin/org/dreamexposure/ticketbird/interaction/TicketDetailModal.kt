package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent
import discord4j.core.`object`.component.TextInput
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
class TicketDetailModal(
    private val ticketService: TicketService,
    private val localeService: LocaleService,
    private val projectService: ProjectService,
    private val ticketCreateStateCache: CacheRepository<String, TicketCreateState>
): InteractionHandler<ModalSubmitInteractionEvent> {
    override val ids = arrayOf("ticket-detail")

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
        event.createFollowup(localeService.getString(
            settings.locale,
            "generic.success.ticket-open",
            ticket.channel.asString()
        )).awaitSingle()
    }
}
