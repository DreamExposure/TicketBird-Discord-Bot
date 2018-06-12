package com.novamaday.ticketbird.listeners;

import com.novamaday.ticketbird.logger.Logger;
import com.novamaday.ticketbird.message.MessageManager;
import com.novamaday.ticketbird.service.TimeManager;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;

public class ReadyEventListener {
    @EventSubscriber
    public void onReadyEvent(ReadyEvent event) {
        Logger.getLogger().debug("Ready!");
        try {
            TimeManager.getManager().init();

            //TODO: Handle Site Updates!
            //UpdateDisBotData.init();
            //UpdateDisPwData.init();

            MessageManager.reloadLangs();

            Logger.getLogger().debug("[ReadyEvent] Connection success!");
        } catch (Exception e) {
            Logger.getLogger().exception(null, "BAD!!!", e, this.getClass());
        }
    }
}