package org.dreamexposure.ticketbird.service

import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.Category
import discord4j.core.`object`.entity.channel.TextChannel
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.dreamexposure.ticketbird.business.EnvironmentService
import org.dreamexposure.ticketbird.business.GuildSettingsService
import org.dreamexposure.ticketbird.business.TicketService
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.utils.GlobalVars.DEFAULT
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
    private val environmentService: EnvironmentService,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        Flux.interval(Duration.ofHours(1))
            .flatMap { doTheThing() }
            .doOnError { LOGGER.error(DEFAULT, "ActivityMonitor | Processor failure ", it) }
            .onErrorResume { Mono.empty() }
            .subscribe()
    }

    private fun doTheThing() = mono {
        LOGGER.debug("Running ticket inactivity close task...")

        client.guilds.collectList().awaitSingle().forEach { guild ->
            val settings = settingsService.getGuildSettings(guild.id)
            if (settings.requiresRepair) return@forEach // Skip processing this guild until they decide to run repair command
            if (!environmentService.validateAllEntitiesExist(settings.guildId)) return@forEach // Skip processing since we know something doesn't exist

            // Isolate errors to guild-level
            try {
                if (settings.hasRequiredIdsSet()) {
                    // Get closed tickets
                    val closedCategoryChannels = guild.getChannelById(settings.closeCategory!!)
                        .ofType(Category::class.java)
                        .onErrorResume { Mono.empty() }
                        .flatMapMany { it.channels.ofType(TextChannel::class.java) }
                        .collectList().awaitSingle()


                    // Get open tickets
                    val awaitingCategoryChannels = guild.getChannelById(settings.awaitingCategory!!)
                        .ofType(Category::class.java)
                        .onErrorResume { Mono.empty() }
                        .flatMapMany { it.channels.ofType(TextChannel::class.java) }
                        .collectList().awaitSingle()
                    val respondedCategoryChannels = guild.getChannelById(settings.respondedCategory!!)
                        .ofType(Category::class.java)
                        .onErrorResume { Mono.empty() }
                        .flatMapMany { it.channels.ofType(TextChannel::class.java) }
                        .collectList().awaitSingle()

                    // Loop closed tickets
                    for (closedTicketChannel in closedCategoryChannels) {
                        val ticket = ticketService.getTicket(guild.id, closedTicketChannel.id)
                        if (ticket != null && Duration.between(Instant.now(), ticket.lastActivity).abs() > Duration.ofDays(1)) {
                            // Ticket closed for over 24 hours, purge
                            ticketService.purgeTicket(settings.guildId, ticket.channel)
                        }
                    }

                    // Loop open tickets
                    for (openTicketChannel in awaitingCategoryChannels + respondedCategoryChannels) {
                        val ticket = ticketService.getTicket(guild.id, openTicketChannel.id)

                        if (ticket != null && Duration.between(Instant.now(), ticket.lastActivity).abs() > Duration.ofDays(7)) {
                            // Inactive, auto-close
                            ticketService.closeTicket(settings.guildId, ticket.channel, inactive = true)
                        }
                    }
                }
            } catch (ex: Exception) {
                LOGGER.error(DEFAULT, "ActivityMonitor Failed to process guild | id: ${guild.id.asString()}", ex)
            }
        }
    }
}
