package org.dreamexposure.ticketbird.listeners.discord

import discord4j.core.event.domain.role.RoleDeleteEvent
import org.dreamexposure.ticketbird.business.GuildSettingsService
import org.springframework.stereotype.Component

@Component
class RoleDeleteListener(
    private val settingsService: GuildSettingsService,
): EventListener<RoleDeleteEvent> {
    override suspend fun handle(event: RoleDeleteEvent) {
        var settings = settingsService.getGuildSettings(event.guildId)

        settings = when (event.roleId) {
            settings.staffRole -> settings.copy(staffRole = null)
            else -> return // Not a role we care about
        }

        // If we made it here, we should update the settings
        settingsService.upsertGuildSettings(settings)
    }
}
