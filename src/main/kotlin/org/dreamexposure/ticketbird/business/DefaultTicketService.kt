package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.database.TicketData
import org.dreamexposure.ticketbird.database.TicketRepository
import org.dreamexposure.ticketbird.`object`.Ticket
import org.springframework.stereotype.Component

@Component
class DefaultTicketService(private val ticketRepository: TicketRepository): TicketService {
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
}
