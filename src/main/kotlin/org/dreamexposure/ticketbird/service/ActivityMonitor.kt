package org.dreamexposure.ticketbird.service

import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.Category
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.rest.http.client.ClientException
import io.netty.handler.codec.http.HttpResponseStatus
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.dreamexposure.ticketbird.business.EnvironmentService
import org.dreamexposure.ticketbird.business.GuildSettingsService
import org.dreamexposure.ticketbird.business.StaticMessageService
import org.dreamexposure.ticketbird.business.TicketService
import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.extensions.asMinutes
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
    private val staticMessageService: StaticMessageService,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        Flux.interval(Config.TIMING_ACTIVITY_MONITOR_FREQUENCY_MINUTES.getLong().asMinutes())
            .flatMap { doTheThing() }
            .doOnError { LOGGER.error(DEFAULT, "ActivityMonitor | Processor failure ", it) }
            .onErrorResume { Mono.empty() }
            .subscribe()
    }

    private fun doTheThing() = mono {
        LOGGER.debug("Running ticket inactivity close task...")

        client.guilds.collectList().awaitSingle().forEach { guild ->
            // Isolate errors to guild-level
            try {
                val settings = settingsService.getGuildSettings(guild.id)
                if (settings.requiresRepair) return@forEach // Skip processing this guild until they decide to run repair command
                if (!environmentService.validateAllEntitiesExist(settings.guildId)) {
                    // Skip processing since we know something doesn't exist
                    staticMessageService.update(settings.guildId)
                    return@forEach
                }

                if (settings.hasRequiredIdsSet()) {
                    var updateStaticMessage = false

                    // Get closed tickets
                    val closedCategoryChannels = guild.getChannelById(settings.closeCategory!!)
                        .ofType(Category::class.java)
                        .flatMapMany { it.channels.ofType(TextChannel::class.java) }
                        .collectList().awaitSingle()


                    // Get open tickets
                    val awaitingCategoryChannels = guild.getChannelById(settings.awaitingCategory!!)
                        .ofType(Category::class.java)
                        .flatMapMany { it.channels.ofType(TextChannel::class.java) }
                        .collectList().awaitSingle()
                    val respondedCategoryChannels = guild.getChannelById(settings.respondedCategory!!)
                        .ofType(Category::class.java)
                        .flatMapMany { it.channels.ofType(TextChannel::class.java) }
                        .collectList().awaitSingle()

                    // Loop closed tickets
                    for (closedTicketChannel in closedCategoryChannels) {
                        val ticket = ticketService.getTicket(guild.id, closedTicketChannel.id) ?: return@forEach
                        if (Duration.between(Instant.now(), ticket.lastActivity).abs() > settings.autoDelete) {
                            // Ticket closed for over 24 hours, purge
                            ticketService.purgeTicket(settings.guildId, ticket.channel)
                            updateStaticMessage = true
                        }
                    }

                    // Loop open tickets
                    for (openTicketChannel in awaitingCategoryChannels + respondedCategoryChannels) {
                        val ticket = ticketService.getTicket(guild.id, openTicketChannel.id) ?: continue

                        try {
                            if (Duration.between(Instant.now(), ticket.lastActivity).abs() > settings.autoClose) {
                                // Inactive, auto-close
                                ticketService.closeTicket(settings.guildId, ticket.channel, inactive = true)
                                updateStaticMessage = true
                            }
                        } catch (ex: ClientException) {
                            if (ex.status == HttpResponseStatus.FORBIDDEN) {
                                // Missing permissions to channel, delete record of ticket as bot can no longer manage it
                                ticketService.deleteTicket(guild.id, ticket.channel)
                            } else throw ex // Rethrow
                        }
                    }

                    if (updateStaticMessage) staticMessageService.update(guild.id)
                }
            } catch (ex: Exception) {
                LOGGER.error(DEFAULT, "ActivityMonitor Failed to process guild | id: ${guild.id.asString()}", ex)
            } finally {
                LOGGER.debug("Ticket inactivity task completed")
            }
        }
    }
}
