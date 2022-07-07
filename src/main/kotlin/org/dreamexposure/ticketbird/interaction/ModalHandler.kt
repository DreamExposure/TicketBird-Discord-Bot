package org.dreamexposure.ticketbird.interaction

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent
import discord4j.core.`object`.entity.Message
import org.dreamexposure.ticketbird.`object`.GuildSettings

interface ModalHandler: InteractionHandler {
    suspend fun handle(event: ModalSubmitInteractionEvent, settings: GuildSettings): Message
}
