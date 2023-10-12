package org.dreamexposure.ticketbird.listeners

import discord4j.core.event.domain.channel.TextChannelDeleteEvent
import org.dreamexposure.ticketbird.business.GuildSettingsService
import org.springframework.stereotype.Component

@Component
class TextChannelDeleteListener(
    private val settingsService: GuildSettingsService,
): EventListener<TextChannelDeleteEvent> {
    override suspend fun handle(event: TextChannelDeleteEvent) {
        val settings = settingsService.getGuildSettings(event.channel.guildId)

        when (event.channel.id) {
            settings.supportChannel -> {
                settings.supportChannel = null
                settings.staticMessage = null // If channel is deleted, so are all messages in it
                settings.requiresRepair = true
            } else -> return // Not a channel we care about
        }

        // If we made it here, we should update the settings
        settingsService.upsertGuildSettings(settings)
    }
}
