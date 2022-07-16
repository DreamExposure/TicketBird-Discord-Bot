package org.dreamexposure.ticketbird.service

import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.Category
import discord4j.core.`object`.entity.channel.TextChannel
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.dreamexposure.ticketbird.business.GuildSettingsService
import org.dreamexposure.ticketbird.business.TicketService
import org.dreamexposure.ticketbird.logger.LOGGER
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant

@Component
class ActivityMonitor(
    private val client: GatewayDiscordClient,
    private val settingsService: GuildSettingsService,
    private val ticketService: TicketService,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        Flux.interval(Duration.ofHours(1))
            .flatMap { doTheThing() }
            .subscribe()
    }

    private fun doTheThing() = mono {
        LOGGER.debug("Running ticket inactivity close task...")

        client.guilds.collectList().awaitSingle().forEach { guild ->
            val settings = settingsService.getGuildSettings(guild.id)
            if (settings.closeCategory != null) {
                // Loop closed tickets
                val closedCategoryChannels = guild.getChannelById(settings.closeCategory!!)
                    .ofType(Category::class.java)
                    .onErrorResume { Mono.empty() }
                    .flatMapMany { it.channels.ofType(TextChannel::class.java) }
                    .collectList().awaitSingle()

                for (closedTicketChannel in closedCategoryChannels) {
                    val ticket = ticketService.getTicket(guild.id, closedTicketChannel.id)
                    if (ticket != null && Duration.between(Instant.now(), ticket.lastActivity).abs() > Duration.ofDays(1)) {
                        // Ticket closed for over 24 hours, purge
                        ticketService.purgeTicket(settings.guildId, ticket.channel)
                    }
                }

                // Loop open tickets
                val awaitingCategoryChannels = guild.getChannelById(settings.awaitingCategory!!)
                    .ofType(Category::class.java)
                    .onErrorResume { Mono.empty() }
                    .flatMapMany { it.channels.ofType(TextChannel::class.java) }
                    .collectList().awaitSingle()
                val respondedCategoryChannels = guild.getChannelById(settings.respondedCategory!!)
                    .ofType(Category::class.java)
                    .flatMapMany { it.channels.ofType(TextChannel::class.java) }
                    .collectList().awaitSingle()

                for (openTicketChannel in awaitingCategoryChannels + respondedCategoryChannels) {
                    val ticket = ticketService.getTicket(guild.id, openTicketChannel.id)

                    if (ticket != null && Duration.between(Instant.now(), ticket.lastActivity).abs() > Duration.ofDays(7)) {
                        // Inactive, auto-close
                        ticketService.closeTicket(settings.guildId, ticket.channel, inactive = true)
                    }
                }
            }
        }
    }
}
