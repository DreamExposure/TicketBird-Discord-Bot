package org.dreamexposure.ticketbird.service

import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.presence.ClientActivity
import discord4j.core.`object`.presence.ClientPresence
import org.dreamexposure.ticketbird.GitProperty
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
        "/ticketbird for info",
        "Version {version}",
        "TicketBird is on Patreon!",
    )

    private fun update(): Mono<Void> {
        val currentIndex = index.get()
        //Update index
        if (currentIndex + 1 >= statuses.size)
            index.lazySet(0)
        else
            index.lazySet(currentIndex + 1)

        //Get status we want to change to
        val status = statuses[currentIndex]
            .replace("{version}", GitProperty.TICKETBIRD_VERSION.value)


        return client.updatePresence(ClientPresence.online(ClientActivity.playing(status)))
    }

    override fun run(args: ApplicationArguments?) {
        Flux.interval(Duration.ofMinutes(5))
            .flatMap { update() }
            .subscribe()
    }
}
