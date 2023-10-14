package org.dreamexposure.ticketbird.listeners

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent
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
class ModalInteractionListener(
    private val settingsService: GuildSettingsService,
    private val localeService: LocaleService,
    private val modals: List<InteractionHandler<ModalSubmitInteractionEvent>>,
    private val metricService: MetricService,
) : EventListener<ModalSubmitInteractionEvent> {

    override suspend fun handle(event: ModalSubmitInteractionEvent) {
        val timer = StopWatch().apply { start() }

        if (!event.interaction.guildId.isPresent) {
            event.reply(localeService.getString(Locale.ENGLISH, "modal.dm-not-supported")).awaitSingleOrNull()
            return
        }

        val modal = modals.firstOrNull { it.ids.contains(event.customId) }

        if (modal != null) {
            try {
                modal.handle(event, settingsService.getGuildSettings(event.interaction.guildId.get()))
            } catch (e: Exception) {
                LOGGER.error(DEFAULT, "Error handling modal interaction | $event", e)

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

        metricService.recordInteractionDuration(event.customId, "modal", timer.totalTimeMillis.apply { timer.stop() })
    }
}
