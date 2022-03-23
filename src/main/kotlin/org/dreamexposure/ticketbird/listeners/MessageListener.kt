@file:Suppress("DuplicatedCode")

package org.dreamexposure.ticketbird.listeners

import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import org.dreamexposure.ticketbird.database.DatabaseManager
import org.dreamexposure.ticketbird.message.MessageManager
import org.dreamexposure.ticketbird.utils.GeneralUtils
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Component
class MessageListener(private val client: GatewayDiscordClient) : ApplicationRunner {

    private fun handle(event: MessageCreateEvent): Mono<Void> {
        // Make sure message is coming from a guild
        if (event.guildId.isEmpty) return Mono.empty()
        // Make sure member object is present, and they are not a bot
        if (event.member.isEmpty || event.member.get().isBot) return Mono.empty()

        val authorId = event.member.get().id

        return Mono.fromCallable {
            DatabaseManager.getManager().getSettings(event.guildId.get())
        }.subscribeOn(Schedulers.boundedElastic()).flatMap { settings ->
            //Check if in support channel, if so, delete
            if (event.message.channelId == settings.supportChannel) {
                // Messages should no longer be sent in this channel, instead a button should be pressed
                return@flatMap event.message.delete()
            }

            // Check if in ticket channel, mono will be empty if it's not a ticket channel
            return@flatMap Mono.fromCallable {
                DatabaseManager.getManager().getTicket(settings.guildId, event.message.channelId)
            }.subscribeOn(Schedulers.boundedElastic()).flatMap { ticket ->
                event.message.channel.ofType(TextChannel::class.java).filter { it.categoryId.isPresent }
                    .flatMap { channel ->
                        val catId = channel.categoryId.get()

                        when {
                            catId == settings.closeCategory -> {
                                // closed - move to correct category, send reopened message, update static msg

                                if (settings.staff.contains(authorId.asString())) {
                                    ticket.category = settings.respondedCategory!!
                                    ticket.lastActivity = System.currentTimeMillis()

                                    val updateDb = Mono.fromCallable {
                                        DatabaseManager.getManager().updateTicket(ticket)
                                    }.subscribeOn(Schedulers.boundedElastic())

                                    Mono.`when`(
                                        channel.edit().withParentIdOrNull(settings.respondedCategory),
                                        channel.createMessage(MessageManager.getMessage("Ticket.Reopen.Everyone", settings)),
                                        updateDb,
                                        event.guild.flatMap { GeneralUtils.updateStaticMessage(it, settings) }
                                    )
                                } else {
                                    ticket.category = settings.awaitingCategory!!
                                    ticket.lastActivity = System.currentTimeMillis()

                                    val updateDb = Mono.fromCallable {
                                        DatabaseManager.getManager().updateTicket(ticket)
                                    }.subscribeOn(Schedulers.boundedElastic())

                                    Mono.`when`(
                                        channel.edit().withParentIdOrNull(settings.awaitingCategory),
                                        channel.createMessage(MessageManager.getMessage("Ticket.Reopen.Everyone", settings)),
                                        updateDb,
                                        event.guild.flatMap { GeneralUtils.updateStaticMessage(it, settings) }
                                    )
                                }
                            }
                            catId == settings.holdCategory -> {
                                // on hold move to correct category and send un-hold message + update static msg
                                if (settings.staff.contains(authorId.asString())) {
                                    ticket.category = settings.respondedCategory!!
                                    ticket.lastActivity = System.currentTimeMillis()

                                    val updateDb = Mono.fromCallable {
                                        DatabaseManager.getManager().updateTicket(ticket)
                                    }.subscribeOn(Schedulers.boundedElastic())

                                    Mono.`when`(
                                        channel.edit().withParentIdOrNull(settings.respondedCategory),
                                        channel.createMessage(MessageManager.getMessage("Ticket.Reopen.Creator", "%creator%", "<@${ticket.creator.asLong()}>", settings)),
                                        updateDb,
                                        event.guild.flatMap { GeneralUtils.updateStaticMessage(it, settings) }
                                    )
                                } else {
                                    ticket.category = settings.awaitingCategory!!
                                    ticket.lastActivity = System.currentTimeMillis()

                                    val updateDb = Mono.fromCallable {
                                        DatabaseManager.getManager().updateTicket(ticket)
                                    }.subscribeOn(Schedulers.boundedElastic())

                                    Mono.`when`(
                                        channel.edit().withParentIdOrNull(settings.awaitingCategory),
                                        channel.createMessage(MessageManager.getMessage("Ticket.Reopen.Creator", "%creator%", "<@${ticket.creator.asLong()}>", settings)),
                                        updateDb,
                                        event.guild.flatMap { GeneralUtils.updateStaticMessage(it, settings) }
                                    )
                                }
                            }
                            catId == settings.awaitingCategory && settings.staff.contains(authorId.asString()) -> {
                                // in awaiting + staff response, move to responded
                                ticket.category = settings.respondedCategory!!
                                ticket.lastActivity = System.currentTimeMillis()

                                val updateDb = Mono.fromCallable {
                                    DatabaseManager.getManager().updateTicket(ticket)
                                }.subscribeOn(Schedulers.boundedElastic())

                                Mono.`when`(
                                    channel.edit().withParentIdOrNull(settings.respondedCategory),
                                    updateDb,
                                )
                            }
                            catId == settings.respondedCategory && !settings.staff.contains(authorId.asString()) -> {
                                // in responded + user response, move to awaiting
                                ticket.category = settings.awaitingCategory!!
                                ticket.lastActivity = System.currentTimeMillis()

                                val updateDb = Mono.fromCallable {
                                    DatabaseManager.getManager().updateTicket(ticket)
                                }.subscribeOn(Schedulers.boundedElastic())

                                Mono.`when`(
                                    channel.edit().withParentIdOrNull(settings.awaitingCategory),
                                    updateDb,
                                )
                            }
                            else -> {
                                ticket.lastActivity = System.currentTimeMillis()

                                Mono.fromCallable {
                                    DatabaseManager.getManager().updateTicket(ticket)
                                }.subscribeOn(Schedulers.boundedElastic())
                            }
                        }
                    }
            }.then()
        }
    }

    override fun run(args: ApplicationArguments?) {
        client.on(MessageCreateEvent::class.java, this::handle).subscribe()
    }
}
