package org.dreamexposure.ticketbird.listeners;

import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.util.Image;
import org.dreamexposure.ticketbird.Main;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.message.MessageManager;
import org.dreamexposure.ticketbird.service.TimeManager;
import org.dreamexposure.ticketbird.utils.GlobalVars;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "ConstantConditions"})
public class ReadyEventListener {

    public static void handle(ReadyEvent event) {
        Logger.getLogger().debug("Ready!");
        try {
            //Start keep-alive
            //KeepAliveHandler.startKeepAlive(60);

            TimeManager.getManager().init();

            GlobalVars.iconUrl = Main.getClient().getApplicationInfo().block().getIcon(Image.Format.PNG).get();

            MessageManager.reloadLangs();

            Logger.getLogger().debug("[ReadyEvent] Connection success! Session ID: " + event.getSessionId());
        } catch (Exception e) {
            Logger.getLogger().exception(null, "BAD!!!", e, ReadyEventListener.class);
        }
    }
}