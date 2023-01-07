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
class HoldTicketButton(
    private val ticketService: TicketService,
    private val staticMessageService: StaticMessageService,
    private val localeService: LocaleService,
): InteractionHandler<ButtonInteractionEvent> {
    override val ids = arrayOf("hold-ticket")

    private val messageDeleteSeconds = Config.TIMING_MESSAGE_DELETE_GENERIC_SECONDS.getLong().asSeconds()

    override suspend fun handle(event: ButtonInteractionEvent, settings: GuildSettings) {
        event.deferReply()
                .withEphemeral(true)
                .awaitSingleOrNull()

        val ticket = ticketService.getTicket(settings.guildId, event.interaction.channelId)

        // Handle if not in a ticket channel
        if (ticket == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.hold.not-ticket"))
                .withEphemeral(true)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }
        // Handle if ticket is already on hold
        if (ticket.category == settings.holdCategory) {
            event.createFollowup(localeService.getString(settings.locale, "command.hold.already-held"))
                .withEphemeral(true)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        // We can place the ticket on hold now
        ticketService.holdTicket(settings.guildId, event.interaction.channelId)
        staticMessageService.update(settings.guildId)

        event.createFollowup(localeService.getString(settings.locale, "command.hold.success"))
            .withEphemeral(true)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }
}
