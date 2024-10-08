package org.dreamexposure.ticketbird.listeners.discord

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
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
class ButtonInteractionListener(
    private val settingsService: GuildSettingsService,
    private val localeService: LocaleService,
    private val buttons: List<InteractionHandler<ButtonInteractionEvent>>,
    private val metricService: MetricService,
) : EventListener<ButtonInteractionEvent> {

    override suspend fun handle(event: ButtonInteractionEvent) {
        val timer = StopWatch()
        timer.start()

        if (!event.interaction.guildId.isPresent) {
            event.reply(localeService.getString(Locale.ENGLISH, "button.dm-not-supported")).awaitSingleOrNull()
            return
        }

        val button = buttons.firstOrNull { it.ids.contains(event.customId) }

        if (button != null) {
            try {
                if (button.shouldDefer(event)) event.deferReply().withEphemeral(button.ephemeral).awaitSingleOrNull()

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

        timer.stop()
        metricService.recordInteractionDuration(event.customId, "button", timer.totalTimeMillis)
    }
}
