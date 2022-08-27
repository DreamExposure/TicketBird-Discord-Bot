package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.InteractionCreateEvent
import org.dreamexposure.ticketbird.`object`.GuildSettings

interface InteractionHandler<T : InteractionCreateEvent> {
    val ids: Array<String>

    suspend fun handle(event: T, settings: GuildSettings)
}
