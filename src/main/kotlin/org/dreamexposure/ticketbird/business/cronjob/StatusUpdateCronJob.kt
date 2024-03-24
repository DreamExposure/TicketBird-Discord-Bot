package org.dreamexposure.ticketbird.business.cronjob

import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.presence.ClientActivity
import discord4j.core.`object`.presence.ClientPresence
import io.micrometer.core.instrument.Tag
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.dreamexposure.ticketbird.GitProperty
import org.dreamexposure.ticketbird.business.MetricService
import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.extensions.asMinutes
import org.dreamexposure.ticketbird.logger.LOGGER
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger

@Component
class StatusUpdateCronJob(
    private val discordClient: GatewayDiscordClient,
    private val metricService: MetricService,
): ApplicationRunner {
    private val index = AtomicInteger(0)

    private final val statuses = listOf(
        "/ticketbird for info & help",
        "Trans rights are human rights",
        "Version {version}",
        "Do you know what a square is?",
        "Now has Interactions!",
        "Proudly written in Kotlin using Discord4J",
        "Free Palestine!",
        "https://ticketbird.dreamexposure.org",
        "Sorting tickets in {guild_count} guilds!",
        "Powered by Discord4J v{d4j_version}",
        "Now supports staff per-topic!",
        "Slava Ukraini!",
        "Support TicketBird on Patreon"
    )

    override fun run(args: ApplicationArguments?) {
        Flux.interval(Config.TIMING_BOT_STATUS_UPDATE_MINUTES.getLong().asMinutes())
            .onBackpressureDrop()
            .flatMap { update() }
            .doOnError { LOGGER.error("Failed to update status", it) }
            .onErrorResume { Mono.empty()}
            .subscribe()
    }

    private fun update() = mono {
        val taskTimer = StopWatch()
        taskTimer.start()


        val currentIndex = index.get()
        //Update index
        if (currentIndex + 1 >= statuses.size) index.lazySet(0)
        else index.lazySet(currentIndex + 1)

        //Get status we want to change to
        var status = statuses[currentIndex]
            .replace("{version}", GitProperty.TICKETBIRD_VERSION.value)
            .replace("{d4j_version}", GitProperty.TICKETBIRD_VERSION_D4J.value)

        if (status.contains("{guild_count}")) {
            val count = discordClient.guilds.count().awaitSingle()
            status = status.replace("{guild_count}", count.toString())
        }


        discordClient.updatePresence(ClientPresence.online(ClientActivity.playing(status))).awaitSingleOrNull()

        taskTimer.stop()
        metricService.recordTaskDuration("status_update", listOf(Tag.of("scope", "cron_job")), taskTimer.totalTimeMillis)
    }
}
