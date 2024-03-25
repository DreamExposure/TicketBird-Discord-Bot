package org.dreamexposure.ticketbird.commands.global

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import kotlinx.coroutines.reactor.awaitSingle
import org.dreamexposure.ticketbird.business.EmbedService
import org.dreamexposure.ticketbird.commands.SlashCommand
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class TicketBirdCommand(
    private val embedService: EmbedService,
): SlashCommand {
    override val name = "ticketbird"
    override val hasSubcommands = false
    override val ephemeral = false

    override suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings) {
        event.createFollowup()
            .withEmbeds(embedService.getTicketBirdInfoEmbed(settings))
            .withEphemeral(ephemeral)
            .awaitSingle()
    }
}
