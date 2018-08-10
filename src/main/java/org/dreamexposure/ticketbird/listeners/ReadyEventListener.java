package org.dreamexposure.ticketbird.listeners;

import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.message.MessageManager;
import org.dreamexposure.ticketbird.network.UpdateDiscordBotsData;
import org.dreamexposure.ticketbird.network.UpdateDiscordPwData;
import org.dreamexposure.ticketbird.service.TimeManager;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;

public class ReadyEventListener {
    @EventSubscriber
    public void onReadyEvent(ReadyEvent event) {
        Logger.getLogger().debug("Ready!");
        try {
            TimeManager.getManager().init();

            //Handle Site Updates!
            UpdateDiscordBotsData.init();
            UpdateDiscordPwData.init();

            MessageManager.reloadLangs();

            Logger.getLogger().debug("[ReadyEvent] Connection success!");
        } catch (Exception e) {
            Logger.getLogger().exception(null, "BAD!!!", e, this.getClass());
        }
    }
}