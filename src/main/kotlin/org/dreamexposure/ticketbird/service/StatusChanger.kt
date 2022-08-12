package org.dreamexposure.ticketbird.service

import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.presence.ClientActivity
import discord4j.core.`object`.presence.ClientPresence
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.dreamexposure.ticketbird.GitProperty
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.utils.GlobalVars.DEFAULT
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

@Component
class StatusChanger(private val client: GatewayDiscordClient) : ApplicationRunner {
    private val index = AtomicInteger(0)

    private val statuses = listOf(
        "/ticketbird for info & help",
        "Do you know what a square is?",
        "Version {version}",
        "Now supports Slash Commands!",
        "Trans rights are human rights",
        "Sorting tickets in {guild_count} guilds!",
        "Proudly written in Kotlin using Discord4J",
        "Slava Ukraini!",
    )

    private fun update() = mono {
        val currentIndex = index.get()
        //Update index
        if (currentIndex + 1 >= statuses.size)
            index.lazySet(0)
        else
            index.lazySet(currentIndex + 1)

        //Get status we want to change to
        var status = statuses[currentIndex]
            .replace("{version}", GitProperty.TICKETBIRD_VERSION.value)

        if (status.contains("{guild_count}")) {
            val count = client.guilds.count().awaitSingle()
            status = status.replace("{guild_count}", "$count")
        }


        client.updatePresence(ClientPresence.online(ClientActivity.playing(status))).awaitSingleOrNull()
    }

    override fun run(args: ApplicationArguments?) {
        Flux.interval(Duration.ofMinutes(5))
            .flatMap { update() }
            .doOnError { LOGGER.error(DEFAULT, "StatusChanger | Processor failure ", it) }
            .onErrorResume { Mono.empty() }
            .subscribe()
    }
}
