package org.dreamexposure.ticketbird.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.extensions.asSeconds
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class TicketCommand(

) : SlashCommand {
    override val name = "ticket"
    override val ephemeral = true

    private val genericMessageDeleteSeconds = Config.TIMING_MESSAGE_DELETE_GENERIC_SECONDS.getLong().asSeconds()
    private val openTicketMessageDeleteSeconds = Config.TIMING_MESSAGE_DELETE_TICKET_FLOW_SECONDS.getLong().asSeconds()

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

        TODO("Not yet implemented")
    }

    private suspend fun open(event: ChatInputInteractionEvent, settings: GuildSettings) {
        TODO("Not yet implemented")
    }

    private suspend fun topic(event: ChatInputInteractionEvent, settings: GuildSettings) {
        TODO("Not yet implemented")
    }

    private suspend fun add(event: ChatInputInteractionEvent, settings: GuildSettings) {
        // TODO: add perms check
        TODO("Not yet implemented")
    }

    private suspend fun remove(event: ChatInputInteractionEvent, settings: GuildSettings) {
        // TODO: add perms check
        TODO("Not yet implemented")
    }

    private suspend fun hold(event: ChatInputInteractionEvent, settings: GuildSettings) {
        TODO("Not yet implemented")
    }

    private suspend fun close(event: ChatInputInteractionEvent, settings: GuildSettings) {
        TODO("Not yet implemented")
    }

    private suspend fun checksum(event: ChatInputInteractionEvent, settings: GuildSettings) {
        TODO("Not yet implemented")
    }
}
