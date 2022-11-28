package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.ComponentService
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.extensions.asSeconds
import org.dreamexposure.ticketbird.extensions.discord4j.deleteReplyDelayed
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class CreateTicketButton(
    private val componentService: ComponentService,
    private val localeService: LocaleService,
): InteractionHandler<ButtonInteractionEvent> {
    override val ids = arrayOf("create-ticket")

    private val messageDeleteSeconds = Config.TIMING_MESSAGE_DELETE_TICKET_FLOW_SECONDS.getLong().asSeconds()

    override suspend fun handle(event: ButtonInteractionEvent, settings: GuildSettings) {
        if (settings.requiresRepair) {
            event.reply(localeService.getString(settings.locale, "generic.repair-required"))
                .withEphemeral(true)
                .awaitSingleOrNull()
            return
        }

        if (settings.useProjects) {
            // Guild is set up to use projects, send an ephemeral select menu to let them select the project
            event.reply(localeService.getString(settings.locale, "dropdown.select-project.prompt"))
                .withComponents(*componentService.getProjectSelectComponents(settings))
                .withEphemeral(true)
                .flatMap { event.deleteReplyDelayed(messageDeleteSeconds) }
                .awaitSingleOrNull()
        } else {
            // Guild is not using projects, send to modal
            event.presentModal()
                .withCustomId("ticket-detail")
                .withTitle(localeService.getString(settings.locale, "modal.ticket-detail.title"))
                .withComponents(*componentService.getTicketOpenModalComponents(settings))
                .awaitSingleOrNull()
        }
    }
}
