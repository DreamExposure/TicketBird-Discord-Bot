package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.ComponentService
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class CreateTicketButton(
    private val componentService: ComponentService,
    private val localeService: LocaleService,
): ButtonHandler {
    override val id = "create-ticket"

    override suspend fun handle(event: ButtonInteractionEvent, settings: GuildSettings) {
        if (settings.useProjects) {
            // Guild is set up to use projects, send an ephemeral select menu to let them select the project
            event.reply(localeService.getString(settings.locale, "dropdown.select-project.prompt"))
                .withComponents(*componentService.getProjectSelectComponents(settings))
                .withEphemeral(true)
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
