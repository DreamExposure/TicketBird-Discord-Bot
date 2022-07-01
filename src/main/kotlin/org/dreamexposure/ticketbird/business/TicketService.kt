package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import org.dreamexposure.ticketbird.`object`.Ticket

interface TicketService {
    suspend fun getTicket(guildId: Snowflake, number: Int): Ticket?

    suspend fun getTicket(guildId: Snowflake, channelId: Snowflake): Ticket?

    suspend fun getAllTickets(guildId: Snowflake): List<Ticket>

    suspend fun createTicket(ticket: Ticket): Ticket

    suspend fun updateTicket(ticket: Ticket)

    suspend fun deleteTicket(guildId: Snowflake, number: Int)

    suspend fun deleteAllTickets(guildId: Snowflake)

    suspend fun closeTicket(guildId: Snowflake, channelId: Snowflake, inactive: Boolean = false)

    suspend fun holdTicket(guildId: Snowflake, channelId: Snowflake)

    suspend fun purgeTicket(guildId: Snowflake, channelId: Snowflake)

    suspend fun moveTicket(guildId: Snowflake, channelId: Snowflake, toCategory: Snowflake, withActivity: Boolean = true)
}
