package org.dreamexposure.ticketbird.interaction.modal

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent
import discord4j.core.`object`.component.TextInput
import discord4j.core.`object`.entity.Message
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.ProjectService
import org.dreamexposure.ticketbird.business.TicketService
import org.dreamexposure.ticketbird.business.state.StateService
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.`object`.TicketCreateState
import org.springframework.stereotype.Component

@Component
class TicketDetailModal(
    private val ticketService: TicketService,
    private val localeService: LocaleService,
    private val projectService: ProjectService,
    private val createStateService: StateService<TicketCreateState>
): ModalHandler {
    override val id = "ticket-detail"

    override suspend fun handle(event: ModalSubmitInteractionEvent, settings: GuildSettings): Message {
        // Defer, it could take a moment
        event.deferReply().withEphemeral(true).awaitSingleOrNull()

        val projectName = createStateService.get(event.interaction.user.id.asString())?.projectName.orEmpty()

        // Create ticket
        val ticket = ticketService.createNewTicketFull(
            guildId = settings.guildId,
            creatorId = event.interaction.user.id,
            project = projectService.getProject(settings.guildId, projectName),
            info = event.getComponents(TextInput::class.java)[0].value.orElse(null)
        )

        // Respond
        return event.createFollowup(localeService.getString(
            settings.locale,
            "modal.ticket-detail.response.success",
            ticket.channel.asString()
        )).awaitSingle()
    }
}
