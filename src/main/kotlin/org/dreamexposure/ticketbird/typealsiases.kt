package org.dreamexposure.ticketbird

import org.dreamexposure.ticketbird.business.cache.CacheRepository
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.`object`.Project
import org.dreamexposure.ticketbird.`object`.Ticket
import org.dreamexposure.ticketbird.`object`.TicketCreateState

// Cache
typealias GuildSettingsCache = CacheRepository<Long, GuildSettings>
typealias TicketCache = CacheRepository<Long, Array<Ticket>>
typealias ProjectCache = CacheRepository<Long, Array<Project>>
typealias TicketCreateStateCache = CacheRepository<String, TicketCreateState>
