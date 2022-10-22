package org.dreamexposure.ticketbird.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.entity.Message
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.StaticMessageService
import org.dreamexposure.ticketbird.business.TicketService
import org.dreamexposure.ticketbird.extensions.asSeconds
import org.dreamexposure.ticketbird.extensions.discord4j.deleteFollowupDelayed
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class HoldCommand(
    private val ticketService: TicketService,
    private val staticMessageService: StaticMessageService,
    private val localeService: LocaleService,
    @Value("\${bot.timing.message-delete.generic.seconds:30}")
    private val messageDeleteSeconds: Long,
): SlashCommand {
    override val name = "hold"
    override val ephemeral = true

    override suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val ticket = ticketService.getTicket(settings.guildId, event.interaction.channelId)

        // Handle if not in a ticket channel
        @Suppress("FoldInitializerAndIfToElvis") // Using == null for readability
        if (ticket == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.hold.not-ticket"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds.asSeconds()) }
                .awaitSingleOrNull()
            return
        }
        // Handle if ticket is already on hold
        if (ticket.category == settings.holdCategory) {
            event.createFollowup(localeService.getString(settings.locale, "command.hold.already-held"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds.asSeconds()) }
                .awaitSingleOrNull()
            return
        }

        // We can place the ticket on hold now
        ticketService.holdTicket(settings.guildId, event.interaction.channelId)
        staticMessageService.update(settings.guildId)

        event.createFollowup(localeService.getString(settings.locale, "command.hold.success"))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds.asSeconds()) }
            .awaitSingleOrNull()
    }
}
