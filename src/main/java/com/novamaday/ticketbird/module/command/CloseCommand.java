package com.novamaday.ticketbird.module.command;

import com.novamaday.ticketbird.database.DatabaseManager;
import com.novamaday.ticketbird.message.MessageManager;
import com.novamaday.ticketbird.objects.command.CommandInfo;
import com.novamaday.ticketbird.objects.guild.GuildSettings;
import com.novamaday.ticketbird.objects.guild.Ticket;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

import java.util.ArrayList;

public class CloseCommand implements ICommand {

    /**
     * Gets the command this Object is responsible for.
     *
     * @return The command this Object is responsible for.
     */
    @Override
    public String getCommand() {
        return "Close";
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
        CommandInfo info = new CommandInfo("Close");
        info.setDescription("Closes the ticket this command is used in.");
        info.setExample("=Close");

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
                //Check if already closed..
                if (!event.getChannel().getCategory().equals(event.getGuild().getCategoryByID(settings.getCloseCategory()))) {
                    //Not closed, lets close it.
                    event.getChannel().changeCategory(event.getGuild().getCategoryByID(settings.getCloseCategory()));

                    //Update database info
                    ticket.setCategory(settings.getCloseCategory());
                    DatabaseManager.getManager().updateTicket(ticket);

                    //Remove command message
                    MessageManager.deleteMessage(event.getMessage());

                    //Send message! :D
                    MessageManager.sendMessage(MessageManager.getMessage("Ticket.Close.Success", "%creator%", event.getGuild().getUserByID(ticket.getCreator()).mention(true), settings), event);
                }
            } else {
                //Not a ticket/invalid ticket.
                MessageManager.sendMessage(MessageManager.getMessage("Ticket.Close.InvalidChannel", settings), event);
            }
        } catch (NumberFormatException | IndexOutOfBoundsException ignore) {
            //Not a ticket channel.
            MessageManager.sendMessage(MessageManager.getMessage("Ticket.Close.InvalidChannel", settings), event);
        }

        return false;
    }
}