package org.dreamexposure.ticketbird.listeners

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.GuildSettingsService
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.interaction.dropdown.SelectMenuHandler
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.utils.GlobalVars.DEFAULT
import org.springframework.stereotype.Component
import java.util.*

@Component
class SelectMenuInteractionListener(
    private val settingsService: GuildSettingsService,
    private val localeService: LocaleService,
    private val dropdowns: List<SelectMenuHandler>
): EventListener<SelectMenuInteractionEvent> {

    override suspend fun handle(event: SelectMenuInteractionEvent) {
        if (!event.interaction.guildId.isPresent) {
            event.reply(localeService.getString(Locale.ENGLISH, "dropdown.dm-not-supported")).awaitSingleOrNull()
            return
        }

        val dropdown = dropdowns.firstOrNull { it.id == event.customId }

        if (dropdown != null) {
            try {
                dropdown.handle(event, settingsService.getGuildSettings(event.interaction.guildId.get()))
            } catch (e: Exception) {
                LOGGER.error(DEFAULT, "Error handling select menu interaction | $event", e)

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

