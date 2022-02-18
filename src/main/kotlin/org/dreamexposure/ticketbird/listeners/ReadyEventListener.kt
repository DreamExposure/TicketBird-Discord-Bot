package org.dreamexposure.ticketbird.listeners

import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.rest.util.Image
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.message.MessageManager
import org.dreamexposure.ticketbird.utils.GlobalVars
import reactor.core.publisher.Mono

object ReadyEventListener {

    fun handle(event: ReadyEvent): Mono<Void> {
        LOGGER.info(GlobalVars.STATUS, "Ready Event  ${event.sessionId}")

        MessageManager.reloadLangs()

        return event.client.applicationInfo
            .map { it.getIconUrl(Image.Format.PNG).orElse("") }
            .doOnNext { GlobalVars.iconUrl = it }
            .then()
    }
}
