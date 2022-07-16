package org.dreamexposure.ticketbird.listeners

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.GuildSettingsService
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.interaction.button.ButtonHandler
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.utils.GlobalVars.DEFAULT
import org.springframework.stereotype.Component
import java.util.*

@Component
class ButtonInteractionListener(
    private val settingsService: GuildSettingsService,
    private val localeService: LocaleService,
    private val buttons: List<ButtonHandler>
): EventListener<ButtonInteractionEvent> {

    override suspend fun handle(event: ButtonInteractionEvent) {
        if (!event.interaction.guildId.isPresent) {
            event.reply(localeService.getString(Locale.ENGLISH, "button.dm-not-supported")).awaitSingleOrNull()
            return
        }

        val button = buttons.firstOrNull { it.id == event.customId }

        if (button != null) {
            try {
                button.handle(event, settingsService.getGuildSettings(event.interaction.guildId.get()))
            } catch (e: Exception) {
                LOGGER.error(DEFAULT, "Error handling button interaction | $event", e)

                // Attempt to provide a message if there's an unhandled exception
                event.createFollowup(localeService.getString(Locale.ENGLISH, "generic.unknown-error"))
                    .withEphemeral(true)
                    .awaitSingleOrNull()
            }
        } else {
            event.createFollowup(localeService.getString(Locale.ENGLISH, "generic.unknown-error"))
                .withEphemeral(true)
                .awaitSingleOrNull()
        }
    }
}
