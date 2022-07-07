package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent
import org.dreamexposure.ticketbird.`object`.GuildSettings

interface SelectMenuHandler: InteractionHandler {
    suspend fun handle(event: SelectMenuInteractionEvent, settings: GuildSettings)
}
