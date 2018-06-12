package com.novamaday.ticketbird.module.status;

import com.novamaday.ticketbird.Main;
import com.novamaday.ticketbird.utils.GlobalVars;
import sx.blah.discord.handle.obj.ActivityType;
import sx.blah.discord.handle.obj.StatusType;

import java.util.ArrayList;
import java.util.TimerTask;

public class StatusChanger extends TimerTask {
    private final ArrayList<String> statuses = new ArrayList<>();
    private Integer index;

    /**
     * Creates the StatusChanger and its Statuses list.
     */
    public StatusChanger() {
        statuses.add("TicketBird");
        statuses.add("=help for help");
        statuses.add("=ticketbird for info");
        statuses.add("Made by NovaFox161");
        statuses.add("Used on %guCount% guilds!");

        statuses.add("%shards% shards!");

        statuses.add("Version " + GlobalVars.version);
        statuses.add("TicketBird is on Patreon!");
        statuses.add("Share TicketBird!!");
        index = 0;
    }

    @Override
    public void run() {
        String status = statuses.get(index);
        status = status.replace("%guCount%", Main.getClient().getGuilds().size() + "");
        status = status.replace("%shards%", Main.getClient().getShardCount() + "");
        Main.getClient().changePresence(StatusType.ONLINE, ActivityType.PLAYING, status);

        //Set new index.
        if (index + 1 >= statuses.size())
            index = 0;
        else
            index++;
    }
}