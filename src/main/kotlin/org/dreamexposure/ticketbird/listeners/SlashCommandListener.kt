package org.dreamexposure.ticketbird.listeners

import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.GuildSettingsService
import org.dreamexposure.ticketbird.command.SlashCommand
import org.springframework.stereotype.Component

@Component
class SlashCommandListener(
    val client: GatewayDiscordClient,
    val settingsService: GuildSettingsService,
    val commands: List<SlashCommand>
): EventListener<ChatInputInteractionEvent> {
    override suspend fun handle(event: ChatInputInteractionEvent) {
        if (!event.interaction.guildId.isPresent) {
            event.reply("Commands not supported in DMs.").awaitSingleOrNull()
            return
        }

        val command = commands.firstOrNull { it.name == event.commandName }

        if (command != null) {
            event.deferReply().withEphemeral(command.ephemeral).awaitSingleOrNull()

            command.handle(event, settingsService.getGuildSettings(event.interaction.guildId.get()))
        } else {
            event.createFollowup("An unknown error occurred").withEphemeral(true).awaitSingleOrNull()
        }
    }
}
