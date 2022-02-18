package org.dreamexposure.ticketbird.service

import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.Category
import discord4j.core.`object`.entity.channel.TextChannel
import org.dreamexposure.ticketbird.database.DatabaseManager
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.message.MessageManager
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class ActivityMonitor(private val client: GatewayDiscordClient) : ApplicationRunner {

    private fun doTheThing(): Mono<Void> {
        LOGGER.debug("Running ticket inactivity close task")
        return client.guilds.flatMap { guild ->
            val settings = DatabaseManager.getManager().getSettings(guild.id)
            if (settings.closeCategory != null) {
                // Loop closed tickets...
                val handleClosed = guild.getChannelById(settings.closeCategory!!)
                    .ofType(Category::class.java)
                    .onErrorResume { Mono.empty() }
                    .flatMapMany {
                        it.channels.ofType(TextChannel::class.java).filter { c -> c.name.contains("-") }
                    }.flatMap { closedTicketChannel ->
                        val id: Int = ticketIdFromName(closedTicketChannel.name)

                        val ticket = DatabaseManager.getManager().getTicket(guild.id, id)
                        if (System.currentTimeMillis() - ticket.lastActivity > Duration.ofDays(1).toMillis()) {
                            // Purge ticket
                            closedTicketChannel.delete("Ticket closed for 24+ hours").doOnNext {
                                settings.totalClosed++
                                DatabaseManager.getManager().removeTicket(settings.guildId, id)
                                DatabaseManager.getManager().updateSettings(settings)
                            }.then()
                        } else Mono.empty()
                    }.onErrorResume(NumberFormatException::class.java) { Mono.empty() }
                    .onErrorResume(IndexOutOfBoundsException::class.java) { Mono.empty() }
                    .then()

                // Loop open tickets
                val handleOpenTickets = Flux.merge(
                    guild.getChannelById(settings.awaitingCategory!!)
                        .ofType(Category::class.java)
                        .onErrorResume { Mono.empty() }
                        .flatMapMany {
                            it.channels.ofType(TextChannel::class.java).filter { c -> c.name.contains("-") }
                        },
                    guild.getChannelById(settings.respondedCategory!!)
                        .ofType(Category::class.java)
                        .onErrorResume { Mono.empty() }
                        .flatMapMany {
                            it.channels.ofType(TextChannel::class.java).filter { c -> c.name.contains("-") }
                        }
                ).flatMap { openTicketChannel ->
                    val id: Int = ticketIdFromName(openTicketChannel.name)

                    val ticket = DatabaseManager.getManager().getTicket(guild.id, id)

                    if (System.currentTimeMillis() - ticket.lastActivity > Duration.ofDays(7).toMillis()) {
                        // Inactive, auto-close
                        openTicketChannel.edit().withParentIdOrNull(settings.closeCategory)
                            .onErrorResume { Mono.empty() }
                            .doOnNext {
                                ticket.category = settings.closeCategory!!
                                DatabaseManager.getManager().updateTicket(ticket)
                            }.flatMap {
                                val msg = MessageManager.getMessage(
                                    "Tickets.Close.Inactive",
                                    "%creator%", "<@${ticket.creator}>",
                                    settings
                                )

                                openTicketChannel.createMessage(msg)
                            }
                    } else Mono.empty()
                }.onErrorResume(NumberFormatException::class.java) {
                    Mono.empty()
                }.onErrorResume(IndexOutOfBoundsException::class.java) {
                    Mono.empty()
                }.then()

                Mono.`when`(handleClosed, handleOpenTickets)
            } else Mono.empty()
        }.then()
    }

    override fun run(args: ApplicationArguments?) {
        Flux.interval(Duration.ofHours(1))
            .flatMap { doTheThing() }
            .subscribe()
    }

    @Throws(NumberFormatException::class, IndexOutOfBoundsException::class)
    private fun ticketIdFromName(name: String): Int {
        return if (name.split("-").size == 2) {
            name.split("-")[1].toInt()
        } else {
            name.split("-")[2].toInt()
        }
    }
}
