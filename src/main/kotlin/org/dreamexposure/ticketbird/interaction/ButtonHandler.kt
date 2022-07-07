package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import org.dreamexposure.ticketbird.`object`.GuildSettings

interface ButtonHandler: InteractionHandler {
    suspend fun handle(event: ButtonInteractionEvent, settings: GuildSettings)
}
