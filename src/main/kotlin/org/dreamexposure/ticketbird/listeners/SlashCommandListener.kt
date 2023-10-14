package org.dreamexposure.ticketbird.listeners

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.GuildSettingsService
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.MetricService
import org.dreamexposure.ticketbird.command.SlashCommand
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.utils.GlobalVars.DEFAULT
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import java.util.*

@Component
class SlashCommandListener(
    private val settingsService: GuildSettingsService,
    private val localeService: LocaleService,
    private val commands: List<SlashCommand>,
    private val metricService: MetricService,
): EventListener<ChatInputInteractionEvent> {
    override suspend fun handle(event: ChatInputInteractionEvent) {
        val timer = StopWatch().apply { start() }

        if (!event.interaction.guildId.isPresent) {
            event.reply(localeService.getString(Locale.ENGLISH, "command.dm-not-supported")).awaitSingleOrNull()
            return
        }

        val command = commands.firstOrNull { it.name == event.commandName }

        if (command != null) {
            event.deferReply().withEphemeral(command.ephemeral).awaitSingleOrNull()

            try {
                command.handle(event, settingsService.getGuildSettings(event.interaction.guildId.get()))
            } catch (e: Exception) {
                LOGGER.error(DEFAULT, "Error handling slash command | $event", e)

                // Attempt to provide a message if there's an unhandled exception
                event.createFollowup(localeService.getString(Locale.ENGLISH, "generic.unknown-error"))
                    .withEphemeral(command.ephemeral)
                    .awaitSingleOrNull()
            }
        } else {
            event.createFollowup(localeService.getString(Locale.ENGLISH, "generic.unknown-error"))
                .withEphemeral(true)
                .awaitSingleOrNull()
        }

        // I lose visibility to sub-command level performance... will want to investigate that eventually
        metricService.recordInteractionDuration(event.commandName, "chat-input", timer.totalTimeMillis.apply { timer.stop() })
    }
}
