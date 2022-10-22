package org.dreamexposure.ticketbird.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import kotlinx.coroutines.reactor.awaitSingle
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.StaticMessageService
import org.dreamexposure.ticketbird.business.TicketService
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class CloseCommand(
    private val ticketService: TicketService,
    private val staticMessageService: StaticMessageService,
    private val localeService: LocaleService,
): SlashCommand {
    override val name = "close"
    override val ephemeral = true

    override suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val ticket = ticketService.getTicket(settings.guildId, event.interaction.channelId)

        // Handle if not in a ticket channel
        @Suppress("FoldInitializerAndIfToElvis") // Using == null for readability
        if (ticket == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.close.not-ticket"))
                .withEphemeral(ephemeral)
                .awaitSingle()
            return
        }
        // Handle if ticket is already closed
        if (ticket.category == settings.closeCategory) {
            event.createFollowup(localeService.getString(settings.locale, "command.close.already-closed"))
                .withEphemeral(ephemeral)
                .awaitSingle()
            return
        }

        // We can close the ticket now
        ticketService.closeTicket(settings.guildId, event.interaction.channelId)
        staticMessageService.update(settings.guildId)

        event.createFollowup(localeService.getString(settings.locale, "command.close.success"))
            .withEphemeral(ephemeral)
            .awaitSingle()
    }
}
