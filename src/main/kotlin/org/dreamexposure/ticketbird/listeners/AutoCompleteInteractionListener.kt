package org.dreamexposure.ticketbird.listeners

import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.GuildSettingsService
import org.dreamexposure.ticketbird.interaction.InteractionHandler
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.utils.GlobalVars.DEFAULT
import org.springframework.stereotype.Component

@Component
class AutoCompleteInteractionListener(
    private val handlers: List<InteractionHandler<ChatInputAutoCompleteEvent>>,
    private val settingsService: GuildSettingsService,
): EventListener<ChatInputAutoCompleteEvent> {
    override suspend fun handle(event: ChatInputAutoCompleteEvent) {
        if (!event.interaction.guildId.isPresent) {
            event.respondWithSuggestions(listOf())
                .awaitSingleOrNull()
            return
        }

        // We use contains since this is meant to be re-usable but Discord uses the app command structure, so we have to handle that
        val id = "${event.commandName}.${event.focusedOption.name}"
        val handler = handlers.firstOrNull { it.id.contains(id, ignoreCase = false) }

        if (handler != null) {
            try {
                handler.handle(event, settingsService.getGuildSettings(event.interaction.guildId.get()))
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
