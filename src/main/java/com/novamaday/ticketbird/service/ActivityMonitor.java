package com.novamaday.ticketbird.service;

import com.novamaday.ticketbird.Main;
import com.novamaday.ticketbird.database.DatabaseManager;
import com.novamaday.ticketbird.logger.Logger;
import com.novamaday.ticketbird.message.MessageManager;
import com.novamaday.ticketbird.objects.guild.GuildSettings;
import com.novamaday.ticketbird.objects.guild.Ticket;
import com.novamaday.ticketbird.utils.GlobalVars;
import sx.blah.discord.handle.obj.IGuild;

import java.util.TimerTask;

public class ActivityMonitor extends TimerTask {


    @Override
    public void run() {
        Logger.getLogger().debug("Running ticket inactivity close task.");
        for (IGuild g : Main.getClient().getGuilds()) {
            GuildSettings settings = DatabaseManager.getManager().getSettings(g.getLongID());

            for (Ticket t : DatabaseManager.getManager().getAllTickets(g.getLongID())) {
                if (t.getCategory() == settings.getRespondedCategory() || t.getCategory() == settings.getAwaitingCategory()) {
                    //Ticket not already closed or on hold.
                    if (System.currentTimeMillis() - t.getLastActivity() > GlobalVars.oneWeekMs) {
                        //Inactive...
                        g.getChannelByID(t.getChannel()).changeCategory(g.getCategoryByID(settings.getCloseCategory()));
                        t.setCategory(settings.getCloseCategory());

                        DatabaseManager.getManager().updateTicket(t);

                        MessageManager.sendMessage(MessageManager.getMessage("Tickets.Close.Inactive", "%creator%", g.getUserByID(t.getCreator()).mention(), settings), g.getChannelByID(t.getChannel()));
                    }
                }
            }
        }
        Logger.getLogger().debug("Finished ticket inactivity close task.");
    }
}