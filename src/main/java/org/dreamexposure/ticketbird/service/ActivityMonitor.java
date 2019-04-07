package org.dreamexposure.ticketbird.service;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.spec.TextChannelEditSpec;
import org.dreamexposure.ticketbird.Main;
import org.dreamexposure.ticketbird.database.DatabaseManager;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.message.ChannelManager;
import org.dreamexposure.ticketbird.message.MessageManager;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import org.dreamexposure.ticketbird.objects.guild.Ticket;
import org.dreamexposure.ticketbird.utils.GlobalVars;
import reactor.core.publisher.Mono;

import java.util.TimerTask;
import java.util.function.Consumer;

public class ActivityMonitor extends TimerTask {


    @Override
    public void run() {
        Logger.getLogger().debug("Running ticket inactivity close task.");
        for (Guild g : Main.getClient().getGuilds().toIterable()) {
            GuildSettings settings = DatabaseManager.getManager().getSettings(g.getId());

            for (Ticket t : DatabaseManager.getManager().getAllTickets(settings.getGuildID())) {
                //Make sure ticket channel exists.
                TextChannel channel = g.getChannelById(t.getChannel()).ofType(TextChannel.class).onErrorResume(e -> Mono.empty()).block();
                if (channel != null) {
                    try {
                        if (t.getCategory() == settings.getRespondedCategory() || t.getCategory() == settings.getAwaitingCategory()) {
                            //Ticket not already closed or on hold.
                            if (System.currentTimeMillis() - t.getLastActivity() > GlobalVars.oneWeekMs) {
                                //Inactive...
                                Consumer<TextChannelEditSpec> editChannel = spec -> spec.setParentId(settings.getCloseCategory());
                                channel.edit(editChannel).subscribe();

                                t.setCategory(settings.getCloseCategory());

                                DatabaseManager.getManager().updateTicket(t);

                                if (g.getMemberById(t.getCreator()).onErrorResume(e -> Mono.empty()).block() != null) {
                                    //noinspection ConstantConditions
                                    MessageManager.sendMessageAsync(MessageManager.getMessage("Tickets.Close.Inactive", "%creator%", g.getMemberById(t.getCreator()).onErrorResume(e -> Mono.empty()).block().getMention(), settings), channel);
                                } else {
                                    MessageManager.sendMessageAsync(MessageManager.getMessage("Tickets.Close.Inactive", "%creator%", "User is Null", settings), channel);
                                }
                            }
                        } else if (t.getCategory() == settings.getCloseCategory()) {
                            //Ticket closed. Check time to purge.
                            if (System.currentTimeMillis() - t.getLastActivity() > GlobalVars.oneDayMs) {
                                //Purge ticket...
                                ChannelManager.deleteCategoryOrChannelAsync(t.getChannel(), g);

                                DatabaseManager.getManager().removeTicket(t.getGuildId(), t.getNumber());

                                settings.setTotalClosed(settings.getTotalClosed() + 1);
                                DatabaseManager.getManager().updateSettings(settings);
                            }
                        }
                    } catch (Exception e) {
                        Logger.getLogger().exception(null, "Failed to handle ticket inactivity!", e, this.getClass());
                    }
                }
            }
        }
        Logger.getLogger().debug("Finished ticket inactivity close/purge task.");
    }
}