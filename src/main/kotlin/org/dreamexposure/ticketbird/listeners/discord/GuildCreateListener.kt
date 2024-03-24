package org.dreamexposure.ticketbird.listeners.discord

import discord4j.core.event.domain.guild.GuildCreateEvent
import org.dreamexposure.ticketbird.business.EnvironmentService
import org.springframework.stereotype.Component

@Component
class GuildCreateListener(
    private val environmentService: EnvironmentService,
): EventListener<GuildCreateEvent> {

    override suspend fun handle(event: GuildCreateEvent) {
        environmentService.registerGuildCommands(event.guild.id)
    }
}
