package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import discord4j.core.`object`.entity.Message
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.StaticMessageService
import org.dreamexposure.ticketbird.business.TicketService
import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.extensions.asSeconds
import org.dreamexposure.ticketbird.extensions.discord4j.deleteFollowupDelayed
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class CloseTicketButton(
    private val ticketService: TicketService,
    private val staticMessageService: StaticMessageService,
    private val localeService: LocaleService,
): InteractionHandler<ButtonInteractionEvent> {
    override val ids = arrayOf("close-ticket")

    private val messageDeleteSeconds = Config.TIMING_MESSAGE_DELETE_GENERIC_SECONDS.getLong().asSeconds()

    override suspend fun handle(event: ButtonInteractionEvent, settings: GuildSettings) {
    event.deferReply()
            .withEphemeral(true)
            .awaitSingleOrNull()

        val ticket = ticketService.getTicket(settings.guildId, event.interaction.channelId)

        // Handle if not in a ticket channel
        if (ticket == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.close.not-ticket"))
                .withEphemeral(true)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }
        // Handle if ticket is already closed
        if (ticket.category == settings.closeCategory) {
            event.createFollowup(localeService.getString(settings.locale, "command.close.already-closed"))
                .withEphemeral(true)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        // We can close the ticket now
        ticketService.closeTicket(settings.guildId, event.interaction.channelId)
        staticMessageService.update(settings.guildId)

        event.createFollowup(localeService.getString(settings.locale, "command.close.success"))
            .withEphemeral(true)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }
}
