package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.ComponentService
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.cache.CacheRepository
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.`object`.TicketCreateState
import org.springframework.stereotype.Component

@Component
class SelectProjectDropdown(
    private val ticketCreateStateCache: CacheRepository<String, TicketCreateState>,
    private val componentService: ComponentService,
    private val localeService: LocaleService,
): InteractionHandler<SelectMenuInteractionEvent> {
    override val id = "select-project"

    override suspend fun handle(event: SelectMenuInteractionEvent, settings: GuildSettings) {
        val selected = event.values[0]

        // Save selected project to state
        ticketCreateStateCache.put("${settings.guildId}.${event.interaction.user.id.asLong()}", TicketCreateState(selected))

        // Pop modal
        event.presentModal()
            .withCustomId("ticket-detail")
            .withTitle(localeService.getString(settings.locale, "modal.ticket-detail.title"))
            .withComponents(*componentService.getTicketOpenModalComponents(settings))
            .awaitSingleOrNull()
    }
}
