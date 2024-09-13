package org.dreamexposure.ticketbird.listeners.discord

import discord4j.core.event.domain.channel.CategoryDeleteEvent
import org.dreamexposure.ticketbird.business.GuildSettingsService
import org.dreamexposure.ticketbird.business.StaticMessageService
import org.springframework.stereotype.Component

@Component
class CategoryDeleteListener(
    private val settingsService: GuildSettingsService,
    private val staticMessageService: StaticMessageService,
): EventListener<CategoryDeleteEvent> {

    override suspend fun handle(event: CategoryDeleteEvent) {
        var settings = settingsService.getGuildSettings(event.category.guildId)

        // Check if any of TicketBird's categories were deleted
        settings = when (event.category.id) {
            settings.awaitingCategory -> settings.copy(awaitingCategory = null, requiresRepair = true)
            settings.respondedCategory -> settings.copy(respondedCategory = null, requiresRepair = true)
            settings.holdCategory -> settings.copy(holdCategory = null, requiresRepair = true)
            settings.closeCategory -> settings.copy(closeCategory = null, requiresRepair = true)
            else -> return // Not a category we care about
        }

        // If we made it here, we should update the settings
        settingsService.upsertGuildSettings(settings)

        if (settings.requiresRepair) staticMessageService.update(settings.guildId)
    }
}
