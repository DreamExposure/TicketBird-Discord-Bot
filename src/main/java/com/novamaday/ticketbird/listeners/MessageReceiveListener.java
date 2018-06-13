package com.novamaday.ticketbird.listeners;

import com.novamaday.ticketbird.Main;
import com.novamaday.ticketbird.database.DatabaseManager;
import com.novamaday.ticketbird.message.ChannelManager;
import com.novamaday.ticketbird.message.MessageManager;
import com.novamaday.ticketbird.objects.guild.GuildSettings;
import com.novamaday.ticketbird.objects.guild.Project;
import com.novamaday.ticketbird.objects.guild.Ticket;
import com.novamaday.ticketbird.utils.GlobalVars;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.EmbedBuilder;

public class MessageReceiveListener {

    @EventSubscriber
    public void onMessageReceive(MessageReceivedEvent event) {
        //Make sure a bot (including us) didn't send the message.
        if (!event.getAuthor().isBot() && event.getAuthor().getLongID() != Main.getClient().getOurUser().getLongID()) {
            //Check if in support request channel
            GuildSettings settings = DatabaseManager.getManager().getSettings(event.getGuild().getLongID());
            if (event.getChannel().getLongID() == settings.getSupportChannel()) {
                //Create a new ticket!
                String content = event.getMessage().getContent();

                int ticketNumber = settings.getNextId();
                settings.setNextId(ticketNumber + 1);
                DatabaseManager.getManager().updateSettings(settings);

                IChannel channel = ChannelManager.createChannel("ticket-" + ticketNumber, event.getGuild());
                channel.changeCategory(event.getGuild().getCategoryByID(settings.getAwaitingCategory()));

                Ticket ticket = new Ticket(event.getGuild().getLongID(), ticketNumber);
                ticket.setChannel(channel.getLongID());
                ticket.setCategory(settings.getAwaitingCategory());
                ticket.setCategory(event.getAuthor().getLongID());
                DatabaseManager.getManager().updateTicket(ticket);

                //Send message
                String msgOr = MessageManager.getMessage("Ticket.Open", settings);
                String msg = msgOr.replace("%creator%", event.getAuthor().mention(true)).replace("%content%", content);

                EmbedBuilder em = new EmbedBuilder();
                em.withAuthorIcon(Main.getClient().getGuildByID(GlobalVars.serverId).getIconURL());
                em.withAuthorName("TicketBird");
                em.withTitle("Select a Project/Service!");
                em.withDesc("Send a message with **ONLY** the project/service's name so we can better help you!");
                for (Project p : DatabaseManager.getManager().getAllProjects(settings.getGuildID())) {
                    em.appendField(p.getName(), p.getPrefix(), true);
                }
                em.withColor(GlobalVars.embedColor);

                MessageManager.sendMessage(em.build(), msg, event);

                //Delete message in support channel.
                event.getMessage().delete();
            } else {
                //Check if in ticket channel...
                try {
                    //Brand new ticket needing project set format ticket-[number]
                    if (event.getChannel().getName().split("-").length == 2) {
                        //New ticket needs project set!!!
                        int ticketNumber = Integer.valueOf(event.getChannel().getName().split("-")[2]);
                        Ticket ticket = DatabaseManager.getManager().getTicket(event.getGuild().getLongID(), ticketNumber);

                        //Check if ticket, if not, fail silently.
                        if (ticket != null) {
                            //Check if message was valid project or not...
                            Project project = DatabaseManager.getManager().getProject(event.getGuild().getLongID(), event.getMessage().getContent());

                            if (project != null) {
                                //Valid project! Lets assign the prefix!
                                event.getChannel().changeName(project.getPrefix() + "-ticket-" + ticket.getNumber());

                                //Update database!
                                ticket.setProject(project.getName());
                                DatabaseManager.getManager().updateTicket(ticket);

                                //Send message...
                                MessageManager.sendMessage(MessageManager.getMessage("Ticket.Project.Success", "%project%", project.getName(), settings), event);
                            } else {
                                //Invalid project.... cannot assign prefix to ticket.
                                MessageManager.sendMessage(MessageManager.getMessage("Ticket.Project.Invalid", settings), event);
                            }
                        }
                    } else {
                        //Existing Ticket channel format: [prefix]-ticket-[number]
                        int ticketNumber = Integer.valueOf(event.getChannel().getName().split("-")[2]);
                        Ticket ticket = DatabaseManager.getManager().getTicket(event.getGuild().getLongID(), ticketNumber);

                        //Check if ticket, if not, fail silently.
                        if (ticket != null) {
                            //It be a ticket, let's handle it!
                            if (event.getChannel().getCategory().getLongID() == settings.getCloseCategory()) {
                                //Ticket was closed, reopen ticket...
                                if (settings.getStaff().contains(event.getAuthor().getLongID())) {
                                    //Staff member responded...

                                    //Move ticket
                                    event.getChannel().changeCategory(event.getGuild().getCategoryByID(settings.getRespondedCategory()));

                                    //Let everyone know it was reopened...
                                    MessageManager.sendMessage(MessageManager.getMessage("Ticket.Reopen.Everyone", settings), event);

                                    //Update database...
                                    ticket.setCategory(settings.getRespondedCategory());
                                    DatabaseManager.getManager().updateTicket(ticket);
                                } else {
                                    //Move ticket...
                                    event.getChannel().changeCategory(event.getGuild().getCategoryByID(settings.getAwaitingCategory()));

                                    //Let everyone know it was reopened...
                                    MessageManager.sendMessage(MessageManager.getMessage("Ticket.Reopen.Everyone", settings), event);

                                    //Update database....
                                    ticket.setCategory(settings.getAwaitingCategory());
                                    DatabaseManager.getManager().updateTicket(ticket);
                                }
                            } else if (event.getChannel().getCategory().getLongID() == settings.getHoldCategory()) {
                                //Ticket was on hold, reopen ticket...
                                if (settings.getStaff().contains(event.getAuthor().getLongID())) {
                                    //Staff member responded...

                                    //Move ticket...
                                    event.getChannel().changeCategory(event.getGuild().getCategoryByID(settings.getRespondedCategory()));

                                    //Let creator know it was reopened...
                                    MessageManager.sendMessage(MessageManager.getMessage("Ticket.Reopen.Creator", "%creator%", event.getGuild().getUserByID(ticket.getCreator()).mention(true), settings), event);

                                    //Update database...
                                    ticket.setCategory(settings.getRespondedCategory());
                                    DatabaseManager.getManager().updateTicket(ticket);
                                } else {
                                    //Move ticket...
                                    event.getChannel().changeCategory(event.getGuild().getCategoryByID(settings.getAwaitingCategory()));

                                    //Let creator know it was reopened...
                                    MessageManager.sendMessage(MessageManager.getMessage("Ticket.Reopen.Creator", "%creator%", event.getGuild().getUserByID(ticket.getCreator()).mention(true), settings), event);

                                    //Update database...
                                    ticket.setCategory(settings.getAwaitingCategory());
                                    DatabaseManager.getManager().updateTicket(ticket);
                                }
                            } else if (event.getChannel().getCategory().getLongID() == settings.getAwaitingCategory()) {
                                //Ticket awaiting response from staff, check user response...
                                if (settings.getStaff().contains(event.getAuthor().getLongID())) {
                                    //Staff member responded...

                                    //Move to responded...
                                    event.getChannel().changeCategory(event.getGuild().getCategoryByID(settings.getRespondedCategory()));

                                    //Update database...
                                    ticket.setCategory(settings.getRespondedCategory());
                                    DatabaseManager.getManager().updateTicket(ticket);
                                }
                            } else if (event.getChannel().getCategory().getLongID() == settings.getRespondedCategory()) {
                                //Ticket responded to by staff, check user response...
                                if (!settings.getStaff().contains(event.getAuthor().getLongID())) {
                                    //Move to awaiting...
                                    event.getChannel().changeCategory(event.getGuild().getCategoryByID(settings.getAwaitingCategory()));

                                    //Update database...
                                    ticket.setCategory(settings.getAwaitingCategory());
                                    DatabaseManager.getManager().updateTicket(ticket);
                                }
                            }
                        }
                    }
                } catch (NumberFormatException | IndexOutOfBoundsException ignore) {
                    //Not in a ticket channel. Fail silently.
                }
            }
        }
    }
}