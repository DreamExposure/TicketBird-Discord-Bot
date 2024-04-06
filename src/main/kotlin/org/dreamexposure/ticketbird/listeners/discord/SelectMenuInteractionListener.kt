package org.dreamexposure.ticketbird.listeners.discord

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.GuildSettingsService
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.MetricService
import org.dreamexposure.ticketbird.interaction.InteractionHandler
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.utils.GlobalVars.DEFAULT
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import java.util.*

@Component
class SelectMenuInteractionListener(
    private val settingsService: GuildSettingsService,
    private val localeService: LocaleService,
    private val dropdowns: List<InteractionHandler<SelectMenuInteractionEvent>>,
    private val metricService: MetricService,
) : EventListener<SelectMenuInteractionEvent> {

    override suspend fun handle(event: SelectMenuInteractionEvent) {
        val timer = StopWatch()
        timer.start()

        if (!event.interaction.guildId.isPresent) {
            event.reply(localeService.getString(Locale.ENGLISH, "dropdown.dm-not-supported")).awaitSingleOrNull()
            return
        }

        val dropdown = dropdowns.firstOrNull { it.ids.contains(event.customId) }

        if (dropdown != null) {
            if (dropdown.shouldDefer(event)) event.deferReply().withEphemeral(dropdown.ephemeral).awaitSingleOrNull()

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

        timer.stop()
        metricService.recordInteractionDuration(event.customId, "select-menu", timer.totalTimeMillis)
    }
}

