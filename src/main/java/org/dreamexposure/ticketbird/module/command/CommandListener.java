package org.dreamexposure.ticketbird.module.command;

import org.dreamexposure.ticketbird.Main;
import org.dreamexposure.ticketbird.database.DatabaseManager;
import org.dreamexposure.ticketbird.listeners.MessageReceiveListener;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;

class CommandListener {
    private CommandExecutor cmd;

    /**
     * Creates a new CommandListener listener.
     *
     * @param _cmd The CommandExecutor instance to use.
     */
    CommandListener(CommandExecutor _cmd) {
        cmd = _cmd;
    }

    /**
     * Checks for command validity and calls the command executor if valid.
     *
     * @param event The event received to check for a command.
     */
    @EventSubscriber
    public void onMessageEvent(MessageReceivedEvent event) {
        try {
            if (event.getMessage() != null && event.getGuild() != null && event.getChannel() != null && !event.getChannel().isPrivate() && event.getMessage().getContent() != null && event.getMessage().getContent().length() > 0) {
                //Message is a valid guild message (not DM). Check if in correct channel.
                GuildSettings settings = DatabaseManager.getManager().getSettings(event.getGuild().getLongID());
                if (event.getMessage().getContent().startsWith(settings.getPrefix())) {
                    //Prefixed with prefix which should mean it is a command, convert and confirm.
                    String[] argsOr = event.getMessage().getContent().split("\\s+");
                    if (argsOr.length > 1) {
                        ArrayList<String> argsOr2 = new ArrayList<>(Arrays.asList(argsOr).subList(1, argsOr.length));
                        String[] args = argsOr2.toArray(new String[argsOr2.size()]);

                        String command = argsOr[0].replace(settings.getPrefix(), "");
                        cmd.issueCommand(command, args, event, settings);
                    } else if (argsOr.length == 1) {
                        //Only command... no args.
                        cmd.issueCommand(argsOr[0].replace(settings.getPrefix(), ""), new String[0], event, settings);
                    }
                } else if (!event.getMessage().mentionsEveryone() && !event.getMessage().mentionsHere() && (event.getMessage().toString().startsWith("<@" + Main.getClient().getOurUser().getStringID() + ">") || event.getMessage().toString().startsWith("<@!" + Main.getClient().getOurUser().getStringID() + ">"))) {
                    String[] argsOr = event.getMessage().getContent().split("\\s+");
                    if (argsOr.length > 2) {
                        ArrayList<String> argsOr2 = new ArrayList<>(Arrays.asList(argsOr).subList(2, argsOr.length));
                        String[] args = argsOr2.toArray(new String[argsOr2.size()]);

                        String command = argsOr[1];
                        cmd.issueCommand(command, args, event, settings);
                    } else if (argsOr.length == 2) {
                        //No args...
                        cmd.issueCommand(argsOr[1], new String[0], event, settings);
                    } else if (argsOr.length == 1) {
                        //Only disCal mentioned...
                        cmd.issueCommand("TicketBird", new String[0], event, settings);
                    }
                } else {
                    //Alright, let's check if its a ticket since its not a command (also prevents oddities with command handling)
                    MessageReceiveListener.onMessageReceive(event, settings);
                }
            }
        } catch (Exception e) {
            Logger.getLogger().exception(event.getAuthor(), "Command error; event message: " + event.getMessage().getContent(), e, this.getClass());
        }
    }
}