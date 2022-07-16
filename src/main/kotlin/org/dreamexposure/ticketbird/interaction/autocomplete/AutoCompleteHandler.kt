package org.dreamexposure.ticketbird.interaction.autocomplete

import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent
import org.dreamexposure.ticketbird.interaction.InteractionHandler

interface AutoCompleteHandler: InteractionHandler {
    suspend fun handle(event: ChatInputAutoCompleteEvent)
}
