package org.dreamexposure.ticketbird.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import org.dreamexposure.ticketbird.business.InteractionService
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.ProjectService
import org.dreamexposure.ticketbird.business.TicketService
import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.extensions.asSeconds
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrNull

@Component
class TicketCommand(
    private val projectService: ProjectService,
    private val ticketService: TicketService,
    private val localeService: LocaleService,
    private val interactionService: InteractionService,
) : SlashCommand {
    override val name = "ticket"
    override val ephemeral = true

    private val genericMessageDeleteSeconds = Config.TIMING_MESSAGE_DELETE_GENERIC_SECONDS.getLong().asSeconds()

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
        val topicId = event.options[0].getOption("topic")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .map(String::toLong)
            .orElse(-1)

        interactionService.openTicketViaCommand(info, topicId, ephemeral, event, settings)
    }

    private suspend fun topic(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val topicId = event.options[0].getOption("topic")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .map(String::toLong)
            .orElse(-1)

        interactionService.changeTopicViaCommand(topicId, ephemeral, event, settings)
    }

    private suspend fun add(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val permission = event.options[0].getOption("permissions")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asLong)
            .get()
        val memberId = event.options[0].getOption("member")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asSnowflake)
            .getOrNull()
        val roleId = event.options[0].getOption("role")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asSnowflake)
            .getOrNull()

        // TODO: add perms check
        TODO("Not yet implemented")
    }

    private suspend fun remove(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val memberId = event.options[0].getOption("member")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asSnowflake)
            .getOrNull()
        val roleId = event.options[0].getOption("role")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asSnowflake)
            .getOrNull()

        // TODO: add perms check
        TODO("Not yet implemented")
    }

    private suspend fun hold(event: ChatInputInteractionEvent, settings: GuildSettings) {
        interactionService.holdTicketViaCommand(ephemeral, event, settings)
    }

    private suspend fun close(event: ChatInputInteractionEvent, settings: GuildSettings) {
        interactionService.closeTicketViaCommand(ephemeral, event, settings)
    }

    private suspend fun checksum(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val transcriptLog = event.options[0].getOption("transcript")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asAttachment)
            .getOrNull()
        val attachmentsZip = event.options[0].getOption("attachments")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asAttachment)
            .getOrNull()


        TODO("Not yet implemented")
    }
}
