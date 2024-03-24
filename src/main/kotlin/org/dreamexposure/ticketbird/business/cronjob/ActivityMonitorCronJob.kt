package org.dreamexposure.ticketbird.business.cronjob

import discord4j.core.GatewayDiscordClient
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.dreamexposure.ticketbird.business.MetricService
import org.dreamexposure.ticketbird.business.TicketService
import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.extensions.asMinutes
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.utils.GlobalVars.DEFAULT
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class ActivityMonitorCronJob(
    private val discordClient: GatewayDiscordClient,
    private val ticketService: TicketService,
    private val metricService: MetricService,
): ApplicationRunner {
    private val interval = Config.TIMING_ACTIVITY_MONITOR_FREQUENCY_MINUTES.getLong().asMinutes()

    override fun run(args: ApplicationArguments?) {
        Flux.interval(interval)
            .onBackpressureDrop()
            .flatMap { doAction() }
            .doOnError { LOGGER.error(DEFAULT, "ActivityMonitorCronJob | Processor failure", it) }
            .onErrorResume { Mono.empty() }
            .subscribe()
    }

    private fun doAction() = mono {
        val taskTimer = StopWatch()
        taskTimer.start()

        val guilds = discordClient.guilds.collectList().awaitSingle()

        guilds.forEach { guild ->
            try {
                ticketService.processTicketActivityForGuild(guild.id)
            } catch (ex: Exception) {
                LOGGER.error("Failed to process ticket activity for guild | guildId:${guild.id.asLong()}", ex)
            }
        }

        taskTimer.stop()
        metricService.recordTicketActivityTaskDuration("cronjob", taskTimer.totalTimeMillis)
    }
}
