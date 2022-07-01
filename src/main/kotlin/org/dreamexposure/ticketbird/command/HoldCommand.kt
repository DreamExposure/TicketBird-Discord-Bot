package org.dreamexposure.ticketbird.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.entity.Message
import kotlinx.coroutines.reactor.awaitSingle
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.StaticMessageService
import org.dreamexposure.ticketbird.business.TicketService
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class HoldCommand(
    private val ticketService: TicketService,
    private val staticMessageService: StaticMessageService,
    private val localeService: LocaleService,
): SlashCommand {
    override val name = "hold"
    override val ephemeral = true

    override suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings): Message {
        val ticket = ticketService.getTicket(settings.guildId, event.interaction.channelId)

        // Handle if not in a ticket channel
        @Suppress("FoldInitializerAndIfToElvis") // Using == null for readability
        if (ticket == null) {
            return event.createFollowup(localeService.getString(settings.locale, "command.hold.not-ticket"))
                .withEphemeral(ephemeral)
                .awaitSingle()
        }
        // Handle if ticket is already on hold
        if (ticket.category == settings.holdCategory) {
            return event.createFollowup(localeService.getString(settings.locale, "command.hold.already-held"))
                .withEphemeral(ephemeral)
                .awaitSingle()
        }

        // We can place the ticket on hold now
        ticketService.holdTicket(settings.guildId, event.interaction.channelId)
        staticMessageService.update(settings.guildId)

        return event.createFollowup(localeService.getString(settings.locale, "command.hold.success"))
            .withEphemeral(ephemeral)
            .awaitSingle()
    }
}
