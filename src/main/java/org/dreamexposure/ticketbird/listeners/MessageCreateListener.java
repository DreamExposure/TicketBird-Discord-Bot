package org.dreamexposure.ticketbird.listeners;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.TextChannelEditSpec;
import org.dreamexposure.ticketbird.Main;
import org.dreamexposure.ticketbird.database.DatabaseManager;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.message.ChannelManager;
import org.dreamexposure.ticketbird.message.MessageManager;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import org.dreamexposure.ticketbird.objects.guild.Project;
import org.dreamexposure.ticketbird.objects.guild.Ticket;
import org.dreamexposure.ticketbird.utils.GeneralUtils;
import org.dreamexposure.ticketbird.utils.GlobalVars;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@SuppressWarnings({"Duplicates", "ConstantConditions"})
public class MessageCreateListener {

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static void onMessageCreate(MessageCreateEvent event, GuildSettings settings) {
        //Make sure a bot (including us) didn't send the message.
        if (event.getMember().isPresent() && !event.getMember().get().isBot() && !event.getMember().get().getId().equals(Main.getClient().getSelfId().get()) && event.getMessage().getContent().isPresent()) {
            //Check if in support request channel
            if (event.getMessage().getChannel().block().getId().equals(settings.getSupportChannel())) {
                //Create a new ticket!
                try {
                    String content = event.getMessage().getContent().get();
                    Guild guild = event.getGuild().block();

                    int ticketNumber = settings.getNextId();
                    settings.setNextId(ticketNumber + 1);
                    DatabaseManager.getManager().updateSettings(settings);

                    TextChannel channel = ChannelManager.createChannel("ticket-" + ticketNumber, "", settings.getAwaitingCategory(), guild);

                    //Set channel permissions...
                    PermissionSet toAdd = PermissionSet.none();
                    toAdd.add(Permission.MENTION_EVERYONE);
                    toAdd.add(Permission.ATTACH_FILES);
                    toAdd.add(Permission.EMBED_LINKS);
                    toAdd.add(Permission.SEND_MESSAGES);
                    toAdd.add(Permission.READ_MESSAGE_HISTORY);
                    PermissionSet toRemove = PermissionSet.all();

                    PermissionOverwrite everyoneOverride = PermissionOverwrite.forRole(guild.getEveryoneRole().block().getId(), PermissionSet.none(), toRemove);
                    PermissionOverwrite forCreator = PermissionOverwrite.forMember(event.getMember().get().getId(), toAdd, PermissionSet.none());

                    channel.addRoleOverwrite(guild.getEveryoneRole().block().getId(), everyoneOverride, "Ticket Created, remove access for everyone").subscribe();
                    channel.addMemberOverwrite(event.getMember().get().getId(), forCreator, "Ticket Created, allow creator access").subscribe();

                    for (Snowflake uid : settings.getStaff()) {
                        if (guild.getMemberById(uid).onErrorResume(e -> Mono.empty()).block() != null) {
                            PermissionOverwrite overwrite = PermissionOverwrite.forMember(uid, toAdd, PermissionSet.none());

                            channel.addMemberOverwrite(uid, overwrite, "Ticket Created, allow staff access").subscribe();
                        }
                    }

                    //Register ticket in database.
                    Ticket ticket = new Ticket(settings.getGuildID(), ticketNumber);
                    ticket.setChannel(channel.getId());
                    ticket.setCategory(settings.getAwaitingCategory());
                    ticket.setCreator(event.getMember().get().getId());
                    ticket.setLastActivity(System.currentTimeMillis());
                    DatabaseManager.getManager().updateTicket(ticket);

                    //Send message
                    String msgOr = MessageManager.getMessage("Ticket.Open", settings);
                    String msg = msgOr.replace("%creator%", event.getMember().get().getMention()).replace("%content%", content);

                    Consumer<EmbedCreateSpec> embed = spec -> {
                        spec.setAuthor("TicketBird", GlobalVars.siteUrl, GlobalVars.iconUrl);
                        spec.setTitle("Select a Project/Service!");
                        spec.setDescription("Send a message with **ONLY** the project/service's name so we can better help you!");
                        for (Project p : DatabaseManager.getManager().getAllProjects(settings.getGuildID())) {
                            spec.addField(p.getName(), "\u200B", false);
                        }
                        spec.setColor(GlobalVars.embedColor);
                    };
                    MessageManager.sendMessageAsync(msg, embed, channel);

                    //Delete message in support channel.
                    event.getMessage().delete().subscribe();

                    //Lets update the static message!
                    GeneralUtils.updateStaticMessage(guild, settings);
                } catch (Exception e) {
                    Logger.getLogger().exception(event.getMember().get(), "Failed to handle new ticket creation!", e, MessageCreateListener.class);
                }
            } else {
                //Check if in ticket channel...
                try {
                    TextChannel channel = event.getMessage().getChannel().ofType(TextChannel.class).block();

                    //Brand new ticket needing project set format ticket-[number]
                    if (channel.getName().split("-").length == 2) {
                        //New ticket needs project set!!!
                        int ticketNumber = Integer.valueOf(channel.getName().split("-")[1]);
                        Ticket ticket = DatabaseManager.getManager().getTicket(settings.getGuildID(), ticketNumber);

                        //Check if ticket, if not, fail silently.
                        if (ticket != null) {
                            //Check if message was valid project or not...
                            Project project = DatabaseManager.getManager().getProject(settings.getGuildID(), event.getMessage().getContent().get());

                            if (project != null) {
                                //Valid project! Lets assign the prefix!

                                //Parse prefix to remove disallowed characters...
                                String prefix = project.getPrefix();

                                for (String s : GlobalVars.disallowed) {
                                    prefix = prefix.replace(s, "");
                                }

                                String finalPrefix = prefix;
                                Consumer<TextChannelEditSpec> editChannel = spec -> spec.setName(finalPrefix.toLowerCase() + "-ticket-" + ticket.getNumber());

                                channel.edit(editChannel).subscribe();

                                //Update database!
                                ticket.setProject(project.getName());
                                ticket.setLastActivity(System.currentTimeMillis());
                                DatabaseManager.getManager().updateTicket(ticket);

                                //Send message...
                                MessageManager.sendMessageAsync(MessageManager.getMessage("Ticket.Project.Success", "%project%", project.getName(), settings), event);
                            } else {
                                //Invalid project.... cannot assign prefix to ticket.
                                MessageManager.sendMessageAsync(MessageManager.getMessage("Ticket.Project.Invalid", settings), event);
                            }
                        }
                    } else {
                        //Existing Ticket channel format: [prefix]-ticket-[number]
                        int ticketNumber = Integer.valueOf(channel.getName().split("-")[2]);
                        Ticket ticket = DatabaseManager.getManager().getTicket(settings.getGuildID(), ticketNumber);

                        //Check if ticket, if not, fail silently.
                        if (ticket != null) {
                            //It be a ticket, let's handle it!
                            if (channel.getCategoryId().get().equals(settings.getCloseCategory())) {
                                //Ticket was closed, reopen ticket...
                                if (settings.getStaff().contains(event.getMember().get().getId())) {
                                    //Staff member responded...

                                    //Move ticket
                                    Consumer<TextChannelEditSpec> editChannel = spec -> spec.setParentId(settings.getRespondedCategory());
                                    channel.edit(editChannel).subscribe();

                                    //Let everyone know it was reopened...
                                    MessageManager.sendMessageAsync(MessageManager.getMessage("Ticket.Reopen.Everyone", settings), event);

                                    //Update database...
                                    ticket.setCategory(settings.getRespondedCategory());
                                    ticket.setLastActivity(System.currentTimeMillis());
                                    DatabaseManager.getManager().updateTicket(ticket);

                                    //Lets update the static message!
                                    GeneralUtils.updateStaticMessage(event.getGuild().block(), settings);
                                } else {
                                    //Move ticket...
                                    Consumer<TextChannelEditSpec> editChannel = spec -> spec.setParentId(settings.getAwaitingCategory());
                                    channel.edit(editChannel).subscribe();

                                    //Let everyone know it was reopened...
                                    MessageManager.sendMessageAsync(MessageManager.getMessage("Ticket.Reopen.Everyone", settings), event);

                                    //Update database....
                                    ticket.setCategory(settings.getAwaitingCategory());
                                    ticket.setLastActivity(System.currentTimeMillis());
                                    DatabaseManager.getManager().updateTicket(ticket);

                                    //Lets update the static message!
                                    GeneralUtils.updateStaticMessage(event.getGuild().block(), settings);
                                }
                            } else if (channel.getCategoryId().get().equals(settings.getHoldCategory())) {
                                //Ticket was on hold, reopen ticket...
                                if (settings.getStaff().contains(event.getMember().get().getId())) {
                                    //Staff member responded...

                                    //Move ticket...
                                    Consumer<TextChannelEditSpec> editChannel = spec -> spec.setParentId(settings.getRespondedCategory());
                                    channel.edit(editChannel).subscribe();

                                    //Let creator know it was reopened...
                                    if (ticket.getCreator() == null) {
                                        MessageManager.sendMessageAsync(MessageManager.getMessage("Ticket.Reopen.Creator", "%creator%", "NO CREATOR", settings), event);
                                    } else {
                                        if (event.getGuild().block().getMemberById(ticket.getCreator()).block() != null) {
                                            MessageManager.sendMessageAsync(MessageManager.getMessage("Ticket.Reopen.Creator", "%creator%", event.getGuild().block().getMemberById(ticket.getCreator()).block().getMention(), settings), event);
                                        } else {
                                            MessageManager.sendMessageAsync(MessageManager.getMessage("Ticket.Reopen.Creator", "%creator%", Main.getClient().getUserById(ticket.getCreator()).block().getMention(), settings), event);
                                        }
                                    }

                                    //Update database...
                                    ticket.setCategory(settings.getRespondedCategory());
                                    ticket.setLastActivity(System.currentTimeMillis());
                                    DatabaseManager.getManager().updateTicket(ticket);

                                    //Lets update the static message!
                                    GeneralUtils.updateStaticMessage(event.getGuild().block(), settings);
                                } else {
                                    //Move ticket...
                                    Consumer<TextChannelEditSpec> editChannel = spec -> spec.setParentId(settings.getAwaitingCategory());
                                    channel.edit(editChannel).subscribe();

                                    //Let creator know it was reopened...
                                    if (ticket.getCreator() == null) {
                                        MessageManager.sendMessageAsync(MessageManager.getMessage("Ticket.Reopen.Creator", "%creator%", "NO CREATOR", settings), event);
                                    } else {
                                        if (event.getGuild().block().getMemberById(ticket.getCreator()).block() != null) {
                                            MessageManager.sendMessageAsync(MessageManager.getMessage("Ticket.Reopen.Creator", "%creator%", event.getGuild().block().getMemberById(ticket.getCreator()).block().getMention(), settings), event);
                                        } else {
                                            MessageManager.sendMessageAsync(MessageManager.getMessage("Ticket.Reopen.Creator", "%creator%", Main.getClient().getUserById(ticket.getCreator()).block().getMention(), settings), event);
                                        }
                                    }

                                    //Update database...
                                    ticket.setCategory(settings.getAwaitingCategory());
                                    ticket.setLastActivity(System.currentTimeMillis());
                                    DatabaseManager.getManager().updateTicket(ticket);

                                    //Lets update the static message!
                                    GeneralUtils.updateStaticMessage(event.getGuild().block(), settings);
                                }
                            } else if (channel.getCategoryId().get().equals(settings.getAwaitingCategory())) {
                                //Ticket awaiting response from staff, check user response...
                                if (settings.getStaff().contains(event.getMember().get().getId())) {
                                    //Staff member responded...

                                    //Move to responded...
                                    Consumer<TextChannelEditSpec> editChannel = spec -> spec.setParentId(settings.getRespondedCategory());
                                    channel.edit(editChannel).subscribe();

                                    //Update database...
                                    ticket.setCategory(settings.getRespondedCategory());
                                    ticket.setLastActivity(System.currentTimeMillis());
                                    DatabaseManager.getManager().updateTicket(ticket);
                                }
                            } else if (channel.getCategoryId().get().equals(settings.getRespondedCategory())) {
                                //Ticket responded to by staff, check user response...
                                if (!settings.getStaff().contains(event.getMember().get().getId())) {
                                    //Move to awaiting...
                                    Consumer<TextChannelEditSpec> editChannel = spec -> spec.setParentId(settings.getAwaitingCategory());
                                    channel.edit(editChannel).subscribe();

                                    //Update database...
                                    ticket.setCategory(settings.getAwaitingCategory());
                                    ticket.setLastActivity(System.currentTimeMillis());
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