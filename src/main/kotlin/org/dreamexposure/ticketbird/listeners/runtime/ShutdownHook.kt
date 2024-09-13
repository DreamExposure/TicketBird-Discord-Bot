package org.dreamexposure.ticketbird.listeners.runtime

import discord4j.core.GatewayDiscordClient
import jakarta.annotation.PreDestroy
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.utils.GlobalVars
import org.springframework.stereotype.Component

@Component
class ShutdownHook(private val discordClient: GatewayDiscordClient) {
    @PreDestroy
    fun onShutdown() {
        LOGGER.info(GlobalVars.STATUS, "Shutting down shard")

        discordClient.logout().subscribe()
    }
}
