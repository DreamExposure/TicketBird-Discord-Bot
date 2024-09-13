package org.dreamexposure.ticketbird.commands.global

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import org.dreamexposure.ticketbird.business.InteractionService
import org.dreamexposure.ticketbird.commands.SlashCommand
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class TicketCommand(
    private val interactionService: InteractionService,
) : SlashCommand {
    override val name = "ticket"
    override val hasSubcommands = true
    override val ephemeral = true

    override suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings) {
        when (event.options[0].name) {
            "open" -> open(event, settings)
            "topic" -> topic(event, settings)
            "add" -> add(event, settings)
            "remove" -> remove(event, settings)
            "hold" -> hold(event, settings)
            "close" -> close(event, settings)
            "checksum" -> checksum(event, settings)
        }
    }

    private suspend fun open(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val info = event.options[0].getOption("info")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .orElse("")
        val topicId = try {
            event.options[0].getOption("topic")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(String::toLong)
                .orElse(-1)
        } catch (ex: NumberFormatException) { -1 }

        interactionService.openTicketViaInteraction(info, topicId, ephemeral, event, settings)
    }

    private suspend fun topic(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val topicId = try {
            event.options[0].getOption("topic")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(String::toLong)
                .orElse(-1)
        } catch (ex: NumberFormatException) { -1 }

        interactionService.changeTopicViaCommand(topicId, ephemeral, event, settings)
    }

    private suspend fun add(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val write = event.options[0].getOption("permissions")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asLong)
            .map { it.toInt() != 0 }
            .get()
        val memberId = event.options[0].getOption("member")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asSnowflake)
            .get()

        interactionService.addParticipantViaCommand(write, memberId, true, event, settings)
    }

    private suspend fun remove(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val memberId = event.options[0].getOption("member")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asSnowflake)
            .get()

        interactionService.removeParticipantViaCommand(memberId, true, event, settings)
    }

    private suspend fun hold(event: ChatInputInteractionEvent, settings: GuildSettings) {
        interactionService.closeTicketViaInteraction(ephemeral, event, settings)
    }

    private suspend fun close(event: ChatInputInteractionEvent, settings: GuildSettings) {
        interactionService.closeTicketViaInteraction(ephemeral, event, settings)
    }

    private suspend fun checksum(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val fileAttachment = event.options[0].getOption("file")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asAttachment)
            .get()

        interactionService.validateChecksumViaInteraction(fileAttachment, ephemeral, event, settings)
    }
}
