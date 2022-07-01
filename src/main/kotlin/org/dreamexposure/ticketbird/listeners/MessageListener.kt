@file:Suppress("DuplicatedCode")

package org.dreamexposure.ticketbird.listeners

import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.channel.TextChannel
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.GuildSettingsService
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.StaticMessageService
import org.dreamexposure.ticketbird.business.TicketService
import org.springframework.stereotype.Component

@Component
class MessageListener(
    private val settingsService: GuildSettingsService,
    private val ticketService: TicketService,
    private val staticMessageService: StaticMessageService,
    private val localeService: LocaleService,
) : EventListener<MessageCreateEvent> {

    override suspend fun handle(event: MessageCreateEvent) {
        // Make sure message is coming from a guild
        if (event.guildId.isEmpty) return
        // Make sure member object is present, and they are not a bot
        if (event.member.isEmpty || event.member.get().isBot) return

        val authorId = event.member.get().id
        val settings = settingsService.getGuildSettings(event.guildId.get())

        // Check if in support channel, if so, delete
        if (event.message.channelId == settings.supportChannel) {
            event.message.delete().awaitSingleOrNull()
            return
        }

        val ticket = ticketService.getTicket(settings.guildId, event.message.channelId) ?: return
        val channel = event.message.channel.ofType(TextChannel::class.java).awaitSingle()
        val catId = if (channel.categoryId.isPresent) channel.categoryId.get() else return

        when {
            catId == settings.closeCategory -> {
                // Closed - move to correct category, send reopened message, update static msg
                val categoryTo = if (settings.staff.contains(authorId.asString())) {
                    settings.respondedCategory!!
                } else {
                    settings.awaitingCategory!!
                }

                ticketService.moveTicket(settings.guildId, channel.id, categoryTo, withActivity = true)
                channel.createMessage(
                        localeService.getString(settings.locale, "ticket.reopen.everyone")
                ).awaitSingleOrNull()
                staticMessageService.update(settings.guildId)
            }
            catId == settings.holdCategory -> {
                // on hold move to correct category and send un-hold message + update static msg
                val categoryTo = if (settings.staff.contains(authorId.asString())) {
                    settings.respondedCategory!! // staff responded
                } else {
                    settings.awaitingCategory!! // non-staff responded
                }


                ticketService.moveTicket(settings.guildId, channel.id, categoryTo, withActivity = true)
                channel.createMessage(
                        localeService.getString(settings.locale, "ticket.reopen.creator", ticket.creator.asString())
                ).awaitSingleOrNull()
                staticMessageService.update(settings.guildId)
            }
            catId == settings.awaitingCategory && settings.staff.contains(authorId.asString()) -> {
                // in awaiting + staff response, move to responded
                ticketService.moveTicket(settings.guildId, channel.id, settings.respondedCategory!!, withActivity = true)
            }
            catId == settings.respondedCategory && !settings.staff.contains(authorId.asString()) -> {
                // in responded + user response, move to awaiting
                ticketService.moveTicket(settings.guildId, channel.id, settings.awaitingCategory!!, withActivity = true)
            }
            else -> {
                // Active ticket, no change in status, update last activity
                ticket.lastActivity = System.currentTimeMillis()
                ticketService.updateTicket(ticket)
            }
        }
    }
}
