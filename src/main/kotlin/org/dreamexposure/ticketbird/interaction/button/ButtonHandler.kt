package org.dreamexposure.ticketbird.interaction.button

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import org.dreamexposure.ticketbird.interaction.InteractionHandler
import org.dreamexposure.ticketbird.`object`.GuildSettings

interface ButtonHandler: InteractionHandler {
    suspend fun handle(event: ButtonInteractionEvent, settings: GuildSettings)
}
