package org.dreamexposure.ticketbird.listeners.discord

import discord4j.core.event.domain.channel.TextChannelDeleteEvent
import org.dreamexposure.ticketbird.business.GuildSettingsService
import org.springframework.stereotype.Component

@Component
class TextChannelDeleteListener(
    private val settingsService: GuildSettingsService,
): EventListener<TextChannelDeleteEvent> {
    override suspend fun handle(event: TextChannelDeleteEvent) {
        var settings = settingsService.getGuildSettings(event.channel.guildId)

        settings = when (event.channel.id) {
            settings.supportChannel -> settings.copy(supportChannel = null, staticMessage = null, requiresRepair = true)
            else -> return // Not a channel we care about
        }

        // If we made it here, we should update the settings
        settingsService.upsertGuildSettings(settings)
    }
}
