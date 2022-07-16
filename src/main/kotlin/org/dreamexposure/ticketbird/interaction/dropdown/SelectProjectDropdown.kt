package org.dreamexposure.ticketbird.interaction.dropdown

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.ComponentService
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.state.StateService
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.`object`.TicketCreateState
import org.springframework.stereotype.Component

@Component
class SelectProjectDropdown(
    private val ticketCreateStateService: StateService<TicketCreateState>,
    private val componentService: ComponentService,
    private val localeService: LocaleService,
): SelectMenuHandler {
    override val id = "select-project"

    override suspend fun handle(event: SelectMenuInteractionEvent, settings: GuildSettings) {
        val selected = event.values[0]

        // Save selected project to state
        ticketCreateStateService.put(event.interaction.user.id.asString(), TicketCreateState(selected))

        // Pop modal
        event.presentModal()
            .withCustomId("ticket-detail")
            .withTitle(localeService.getString(settings.locale, "modal.ticket-detail.title"))
            .withComponents(*componentService.getTicketOpenModalComponents(settings))
            .awaitSingleOrNull()
    }
}
