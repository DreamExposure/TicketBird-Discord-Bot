package org.dreamexposure.ticketbird

import discord4j.common.util.Snowflake
import org.dreamexposure.ticketbird.business.cache.CacheRepository
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.`object`.Project
import org.dreamexposure.ticketbird.`object`.Ticket
import org.dreamexposure.ticketbird.`object`.TicketCreateState

// Cache
typealias GuildSettingsCache = CacheRepository<Snowflake, GuildSettings>
typealias TicketCache = CacheRepository<Snowflake, Ticket>
typealias ProjectCache = CacheRepository<Snowflake, Array<Project>>
typealias TicketCreateStateCache = CacheRepository<Snowflake, TicketCreateState>
