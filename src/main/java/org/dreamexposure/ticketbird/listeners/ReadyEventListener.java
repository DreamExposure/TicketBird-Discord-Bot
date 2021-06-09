package org.dreamexposure.ticketbird.listeners;

import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.rest.util.Image;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.message.MessageManager;
import org.dreamexposure.ticketbird.service.TimeManager;
import org.dreamexposure.ticketbird.utils.GlobalVars;
import reactor.core.publisher.Mono;

public class ReadyEventListener {
    public static Mono<Void> handle(ReadyEvent event) {
        return Mono.defer(() -> {
            Logger.getLogger().debug("Ready!", false);

            TimeManager.getManager().init();

            MessageManager.reloadLangs();

            Logger.getLogger().debug("[ReadyEvent] Connection success! Session ID: " + event.getSessionId(), false);

            return event.getClient().getApplicationInfo()
                .map(info -> info.getIconUrl(Image.Format.PNG).orElse(""))
                .doOnNext(s -> GlobalVars.iconUrl = s)
                .then();
        });
    }
}
