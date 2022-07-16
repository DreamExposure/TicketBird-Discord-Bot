package org.dreamexposure.ticketbird.interaction.button

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.StaticMessageService
import org.dreamexposure.ticketbird.business.TicketService
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class CloseTicketButton(
    private val ticketService: TicketService,
    private val staticMessageService: StaticMessageService,
    private val localeService: LocaleService,
): ButtonHandler {
    override val id = "close-ticket"
    override suspend fun handle(event: ButtonInteractionEvent, settings: GuildSettings) {
    event.deferReply()
            .withEphemeral(true)
            .awaitSingleOrNull()

        val ticket = ticketService.getTicket(settings.guildId, event.interaction.channelId)

        // Handle if not in a ticket channel
        @Suppress("FoldInitializerAndIfToElvis") // Using == null for readability
        if (ticket == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.close.not-ticket"))
                .withEphemeral(true)
                .awaitSingle()
            return
        }
        // Handle if ticket is already closed
        if (ticket.category == settings.closeCategory) {
            event.createFollowup(localeService.getString(settings.locale, "command.close.already-closed"))
                .withEphemeral(true)
                .awaitSingle()
            return
        }

        // We can close the ticket now
        ticketService.closeTicket(settings.guildId, event.interaction.channelId)
        staticMessageService.update(settings.guildId)

        event.createFollowup(localeService.getString(settings.locale, "command.close.success"))
            .withEphemeral(true)
            .awaitSingle()
    }
}
