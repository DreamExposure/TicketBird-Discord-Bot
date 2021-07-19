package org.dreamexposure.ticketbird.module.status;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import org.dreamexposure.ticketbird.TicketBird;

import java.util.ArrayList;
import java.util.TimerTask;

import static org.dreamexposure.ticketbird.GitProperty.TICKETBIRD_VERSION;

public class StatusChanger extends TimerTask {
    private final ArrayList<String> statuses = new ArrayList<>();
    private int index;

    private final GatewayDiscordClient client;

    /**
     * Creates the StatusChanger and its Statuses list.
     */
    public StatusChanger(GatewayDiscordClient client) {
        this.client = client;

        statuses.add("TicketBird Bot!");
        statuses.add("=help for help");
        statuses.add("=ticketbird for info");
        statuses.add("Powered by DreamExposure");
        statuses.add("Used on %guCount% guilds!");
        statuses.add("%shards% shards!");
        statuses.add("Version " + TICKETBIRD_VERSION.getValue());
        statuses.add("TicketBird is on Patreon!");
        statuses.add("Share TicketBird!!");
        index = 0;
    }

    @Override
    public void run() {
        String status = statuses.get(index);

        if (status.contains("%guCount%"))
            status = status.replace("%guCount%", client.getGuilds().count().block() + "");
        status = status.replace("%shards%", TicketBird.getShardCount() + "");

        client.updatePresence(Presence.online(Activity.playing(status))).subscribe();

        //Set new index.
        if (index + 1 >= statuses.size())
            index = 0;
        else
            index++;
    }
}
