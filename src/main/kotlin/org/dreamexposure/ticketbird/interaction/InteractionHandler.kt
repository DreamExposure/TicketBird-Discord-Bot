package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.InteractionCreateEvent
import org.dreamexposure.ticketbird.`object`.GuildSettings

interface InteractionHandler<T : InteractionCreateEvent> {
    val ids: Array<String>
    val ephemeral: Boolean

    suspend fun shouldDefer(event: T): Boolean = true

    suspend fun handle(event: T, settings: GuildSettings)
}
