package org.dreamexposure.ticketbird.module.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.TextChannelEditSpec;
import org.dreamexposure.ticketbird.database.DatabaseManager;
import org.dreamexposure.ticketbird.message.MessageManager;
import org.dreamexposure.ticketbird.objects.command.CommandInfo;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import org.dreamexposure.ticketbird.objects.guild.Ticket;
import org.dreamexposure.ticketbird.utils.GeneralUtils;

import java.util.ArrayList;
import java.util.function.Consumer;

@SuppressWarnings({"ConstantConditions", "OptionalGetWithoutIsPresent"})
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
    public boolean issueCommand(String[] args, MessageCreateEvent event, GuildSettings settings) {
        TextChannel channel = event.getMessage().getChannel().ofType(TextChannel.class).block();
        Ticket ticket = DatabaseManager.getManager().getTicket(settings.getGuildID(), event.getMessage().getChannelId());

        if (ticket != null) {
            //Check if already closed or on hold.
            if (!channel.getCategoryId().get().equals(settings.getCloseCategory()) && !channel.getCategoryId().get().equals(settings.getHoldCategory())) {
                //Not closed or on hold. Let's place it on hold!
                Consumer<TextChannelEditSpec> editChannel = spec -> spec.setParentId(settings.getHoldCategory());
                channel.edit(editChannel).subscribe();

                //Update database info
                ticket.setCategory(settings.getHoldCategory());
                DatabaseManager.getManager().updateTicket(ticket);

                //Remove command message
                MessageManager.deleteMessage(event.getMessage());

                //Send message! :D
                if (ticket.getCreator() == null) {
                    MessageManager.sendMessageAsync(MessageManager.getMessage("Ticket.Hold.Success", "%creator%", "NO CREATOR", settings), event);
                } else {
                    MessageManager.sendMessageAsync(MessageManager.getMessage("Ticket.Hold.Success", "%creator%", event.getGuild().block().getMemberById(ticket.getCreator()).block().getMention(), settings), event);
                }

                //Lets update the static message!
                GeneralUtils.updateStaticMessage(event.getGuild().block(), settings);
            }
        } else {
            //Not a ticket/invalid ticket.
            MessageManager.sendMessageAsync(MessageManager.getMessage("Ticket.Hold.InvalidChannel", settings), event);
        }
        return false;
    }
}
