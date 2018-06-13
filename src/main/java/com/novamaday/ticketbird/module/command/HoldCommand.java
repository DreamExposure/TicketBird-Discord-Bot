package com.novamaday.ticketbird.module.command;

import com.novamaday.ticketbird.database.DatabaseManager;
import com.novamaday.ticketbird.message.MessageManager;
import com.novamaday.ticketbird.objects.command.CommandInfo;
import com.novamaday.ticketbird.objects.guild.GuildSettings;
import com.novamaday.ticketbird.objects.guild.Ticket;
import com.novamaday.ticketbird.utils.GeneralUtils;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

import java.util.ArrayList;

public class HoldCommand implements ICommand {

    /**
     * Gets the command this Object is responsible for.
     *
     * @return The command this Object is responsible for.
     */
    @Override
    public String getCommand() {
        return "Hold";
    }

    /**
     * Gets the short aliases of the command this object is responsible for.
     * </br>
     * This will return an empty ArrayList if none are present
     *
     * @return The aliases of the command.
     */
    @Override
    public ArrayList<String> getAliases() {
        return new ArrayList<>();
    }

    /**
     * Gets the info on the command (not sub command) to be used in help menus.
     *
     * @return The command info.
     */
    @Override
    public CommandInfo getCommandInfo() {
        CommandInfo info = new CommandInfo("Hold");
        info.setDescription("Puts the ticket on hold until a staff member can handle it. Held tickets are not auto closed.");
        info.setExample("=Hold");

        return info;
    }

    /**
     * Issues the command this Object is responsible for.
     *
     * @param args  The command arguments.
     * @param event The event received.
     * @return <code>true</code> if successful, else <code>false</code>.
     */
    @Override
    public Boolean issueCommand(String[] args, MessageReceivedEvent event, GuildSettings settings) {
        String channelName = event.getChannel().getName();
        //Channel name format [prefix]-ticket-[number]
        try {
            int ticketNumber = Integer.valueOf(channelName.split("-")[2]);
            Ticket ticket = DatabaseManager.getManager().getTicket(event.getGuild().getLongID(), ticketNumber);

            if (ticket != null) {
                //Check if already closed or on hold.
                if (!event.getChannel().getCategory().equals(event.getGuild().getCategoryByID(settings.getCloseCategory())) && !event.getChannel().getCategory().equals(event.getGuild().getCategoryByID(settings.getHoldCategory()))) {
                    //Not closed or on hold. Let's place it on hold!
                    event.getChannel().changeCategory(event.getGuild().getCategoryByID(settings.getHoldCategory()));

                    //Update database info
                    ticket.setCategory(settings.getHoldCategory());
                    DatabaseManager.getManager().updateTicket(ticket);

                    //Remove command message
                    MessageManager.deleteMessage(event.getMessage());

                    //Send message! :D
                    MessageManager.sendMessage(MessageManager.getMessage("Ticket.Hold.Success", "%creator%", event.getGuild().getUserByID(ticket.getCreator()).mention(), settings), event);

                    //Lets update the static message!
                    GeneralUtils.updateStaticMessage(event.getGuild(), settings);
                }
            } else {
                //Not a ticket/invalid ticket.
                MessageManager.sendMessage(MessageManager.getMessage("Ticket.Hold.InvalidChannel", settings), event);
            }
        } catch (NumberFormatException | IndexOutOfBoundsException ignore) {
            //Not a ticket channel.
            MessageManager.sendMessage(MessageManager.getMessage("Ticket.Hold.InvalidChannel", settings), event);
        }
        return false;
    }
}