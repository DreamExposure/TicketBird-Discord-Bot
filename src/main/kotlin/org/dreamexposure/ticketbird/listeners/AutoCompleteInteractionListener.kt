package org.dreamexposure.ticketbird.listeners

import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.interaction.autocomplete.AutoCompleteHandler
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.utils.GlobalVars.DEFAULT
import org.springframework.stereotype.Component

@Component
class AutoCompleteInteractionListener(
    private val handlers: List<AutoCompleteHandler>
): EventListener<ChatInputAutoCompleteEvent> {
    override suspend fun handle(event: ChatInputAutoCompleteEvent) {
        if (!event.interaction.guildId.isPresent) {
            event.respondWithSuggestions(listOf())
                .awaitSingleOrNull()
            return
        }

        val handler = handlers.firstOrNull { it.id == event.commandName }

        if (handler != null) {
            try {
                handler.handle(event)
            } catch (e: Exception) {
                LOGGER.error(DEFAULT, "Error handling auto complete interaction | $event", e)

                // Attempt to respond with empty list so user doesn't have to wait
                event.respondWithSuggestions(listOf())
                    .awaitSingleOrNull()
            }
        } else {
            // No handler, respond empty until support is added
            event.respondWithSuggestions(listOf())
                .awaitSingleOrNull()
        }
    }
}
