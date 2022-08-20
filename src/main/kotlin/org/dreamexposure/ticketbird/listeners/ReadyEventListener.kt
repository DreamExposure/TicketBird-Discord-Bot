package org.dreamexposure.ticketbird.listeners

import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.rest.util.Image
import kotlinx.coroutines.reactor.awaitSingle
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.utils.GlobalVars
import org.springframework.stereotype.Component

@Component
class ReadyEventListener: EventListener<ReadyEvent> {

    override suspend fun handle(event: ReadyEvent) {
        LOGGER.info(GlobalVars.STATUS, "Ready Event  ${event.sessionId}")

        val iconUrl = event.client.applicationInfo.map { it.getIconUrl(Image.Format.PNG).orElse("") }.awaitSingle()
        GlobalVars.iconUrl = iconUrl
    }
}
