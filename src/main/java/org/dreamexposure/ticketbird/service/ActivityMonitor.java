package org.dreamexposure.ticketbird.service;

import org.dreamexposure.ticketbird.Main;
import org.dreamexposure.ticketbird.database.DatabaseManager;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.message.ChannelManager;
import org.dreamexposure.ticketbird.message.MessageManager;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import org.dreamexposure.ticketbird.objects.guild.Ticket;
import org.dreamexposure.ticketbird.utils.GlobalVars;

import java.util.List;
import java.util.TimerTask;
import java.util.function.Consumer;

import discord4j.core.object.entity.Category;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.spec.TextChannelEditSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ActivityMonitor extends TimerTask {

    @Override
    public void run() {
        Logger.getLogger().debug("Running ticket inactivity close task.", false);
        Main.getClient().getGuilds().doOnNext(g -> {
            try {
                GuildSettings settings = DatabaseManager.getManager().getSettings(g.getId());
                if (settings.getCloseCategory() != null) {
                    //Loop closed tickets...
                    List<TextChannel> closed = g
                            .getChannelById(settings.getCloseCategory())
                            .ofType(Category.class)
                            .onErrorResume(m -> Mono.empty())
                            .flatMapMany(c -> c.getChannels()
                                    .ofType(TextChannel.class)
                                    .filter(tc -> tc.getName().contains("-")))
                            .collectList().block();
                    if (closed != null) {
                        for (TextChannel tc : closed) {
                            try {
                                int id;
                                if (tc.getName().split("-").length == 2)
                                    id = Integer.parseInt(tc.getName().split("-")[1]);
                                else
                                    id = Integer.parseInt(tc.getName().split("-")[2]);

                                //Get from database and check time
                                Ticket tic = DatabaseManager.getManager().getTicket(settings.getGuildID(), id);
                                if (System.currentTimeMillis() - tic.getLastActivity() > GlobalVars.oneDayMs) {
                                    //Purge ticket...
                                    ChannelManager.deleteCategoryOrChannelAsync(tc.getId(), g);

                                    DatabaseManager.getManager().removeTicket(settings.getGuildID(), id);

                                    settings.setTotalClosed(settings.getTotalClosed() + 1);
                                    DatabaseManager.getManager().updateSettings(settings);
                                }
                            } catch (NumberFormatException ignore) { }
                        }
                    }

                    //Loop open tickets
                    List<TextChannel> open = Flux.merge(
                            g.getChannelById(settings.getAwaitingCategory())
                                    .ofType(Category.class)
                                    .onErrorResume(m -> Mono.empty())
                                    .flatMapMany(c -> c.getChannels()
                                            .ofType(TextChannel.class)
                                            .filter(tc -> tc.getName().contains("-"))),
                            g.getChannelById(settings.getRespondedCategory())
                                    .ofType(Category.class)
                                    .onErrorResume(m -> Mono.empty())
                                    .flatMapMany(c -> c.getChannels()
                                            .ofType(TextChannel.class)
                                            .filter(tc -> tc.getName().contains("-")))
                    ).collectList().block();
                    if (open != null) {
                        for (TextChannel tc : open) {
                            try {
                                int id;
                                if (tc.getName().split("-").length == 2)
                                    id = Integer.parseInt(tc.getName().split("-")[1]);
                                else
                                    id = Integer.parseInt(tc.getName().split("-")[2]);

                                //Get from database and check time and handle that shit
                                Ticket tic = DatabaseManager.getManager().getTicket(settings.getGuildID(), id);

                                if (System.currentTimeMillis() - tic.getLastActivity() > GlobalVars.oneWeekMs) {
                                    //Inactive
                                    Consumer<TextChannelEditSpec> editChannel = spec -> spec.setParentId(settings.getCloseCategory());
                                    tc.edit(editChannel).subscribe();

                                    tic.setCategory(settings.getCloseCategory());

                                    DatabaseManager.getManager().updateTicket(tic);

                                    if (g.getMemberById(tic.getCreator()).onErrorResume(e -> Mono.empty()).block() != null) {
                                        //noinspection ConstantConditions
                                        MessageManager.sendMessageAsync(MessageManager.getMessage("Tickets.Close.Inactive", "%creator%", g.getMemberById(tic.getCreator()).onErrorResume(e -> Mono.empty()).block().getMention(), settings), tc);
                                    } else {
                                        MessageManager.sendMessageAsync(MessageManager.getMessage("Tickets.Close.Inactive", "%creator%", "User is Null", settings), tc);
                                    }
                                }
                            } catch (NumberFormatException ignore) { }
                        }
                    }
                }
            } catch (Exception e) {
                Logger.getLogger().exception(null, "Ticket Activity Handler Failure", e, true, getClass());
            }
        }).onErrorResume(e -> Mono.empty()).subscribe();
    }
}
