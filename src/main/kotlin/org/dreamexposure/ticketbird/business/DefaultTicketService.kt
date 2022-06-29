package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.TextChannel
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.database.TicketData
import org.dreamexposure.ticketbird.database.TicketRepository
import org.dreamexposure.ticketbird.`object`.Ticket
import org.springframework.stereotype.Component

@Component
class DefaultTicketService(
    private val ticketRepository: TicketRepository,
    private val discordClient: GatewayDiscordClient,
    private val settingsService: GuildSettingsService,
    private val localeService: LocaleService,
) : TicketService {
    override suspend fun getTicket(guildId: Snowflake, number: Int): Ticket? {
        return ticketRepository.findByGuildIdAndNumber(guildId.asLong(), number)
            .map(::Ticket)
            .awaitSingleOrNull()
    }

    override suspend fun getTicket(guildId: Snowflake, channelId: Snowflake): Ticket? {
        return ticketRepository.findByGuildIdAndChannel(guildId.asLong(), channelId.asLong())
            .map(::Ticket)
            .awaitSingleOrNull()
    }

    override suspend fun getAllTickets(guildId: Snowflake): List<Ticket> {
        return ticketRepository.findByGuildId(guildId.asLong())
            .map(::Ticket)
            .collectList()
            .awaitSingle()
    }

    override suspend fun createTicket(ticket: Ticket): Ticket {
        return ticketRepository.save(TicketData(
            guildId = ticket.guildId.asLong(),
            number = ticket.number,
            project = ticket.project,
            creator = ticket.creator.asLong(),
            channel = ticket.channel.asLong(),
            category = ticket.category.asLong(),
            lastActivity = ticket.lastActivity,
        )).map(::Ticket).awaitSingle()
    }

    override suspend fun updateTicket(ticket: Ticket) {
        ticketRepository.updateByGuildIdAndNumber(
            guildId = ticket.guildId.asLong(),
            number = ticket.number,
            project = ticket.project,
            creator = ticket.creator.asLong(),
            channel = ticket.channel.asLong(),
            category = ticket.category.asLong(),
            lastActivity = ticket.lastActivity
        ).awaitSingleOrNull()
    }

    override suspend fun deleteTicket(guildId: Snowflake, number: Int) {
        ticketRepository.deleteByGuildIdAndNumber(guildId.asLong(), number).awaitSingleOrNull()
    }

    override suspend fun deleteAllTickets(guildId: Snowflake) {
        ticketRepository.deleteAllByGuildId(guildId.asLong()).awaitSingleOrNull()
    }

    override suspend fun closeTicket(guildId: Snowflake, channelId: Snowflake, inactive: Boolean) {
        val ticket = getTicket(guildId, channelId) ?: return // return if ticket does not exist
        val settings = settingsService.getGuildSettings(guildId)
        val channel = discordClient.getChannelById(channelId).ofType(TextChannel::class.java).awaitSingle()

        channel.edit().withParentIdOrNull(settings.closeCategory)
            .doOnNext { ticket.category = settings.closeCategory!! }
            .awaitSingle()
        updateTicket(ticket)

        val field = if (inactive) "ticket.close.inactive" else "ticket.close.generic"


        channel.createMessage(localeService.getString(settings.locale, field, ticket.creator.asString())).awaitSingleOrNull()
    }

    override suspend fun purgeTicket(guildId: Snowflake, channelId: Snowflake) {
        val ticket = getTicket(guildId, channelId) ?: return // return if ticket does not exist
        val settings = settingsService.getGuildSettings(guildId)
        val channel = discordClient.getChannelById(channelId).ofType(TextChannel::class.java).awaitSingle()

        channel.delete(localeService.getString(settings.locale, "ticket.delete.time")).awaitSingleOrNull()
        deleteTicket(guildId, ticket.number)
    }
}
