package org.dreamexposure.ticketbird.commands.global

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import org.dreamexposure.ticketbird.business.InteractionService
import org.dreamexposure.ticketbird.commands.SlashCommand
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class TopicCommand(
    private val interactionService: InteractionService,
): SlashCommand {
    override val name = "topic"
    override val hasSubcommands = false
    override val ephemeral = true

    override suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val topicId = try {
            event.getOption("topic")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(String::toLong)
                .orElse(-1)
        } catch (ex: NumberFormatException) { -1 }

        interactionService.changeTopicViaCommand(topicId, ephemeral, event, settings)
    }
}
