package org.dreamexposure.ticketbird.module.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import org.dreamexposure.ticketbird.Main;
import org.dreamexposure.ticketbird.database.DatabaseManager;
import org.dreamexposure.ticketbird.listeners.MessageCreateListener;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;

import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings({"ToArrayCallWithZeroLengthArrayArgument", "OptionalGetWithoutIsPresent"})
class CommandListener {

    /**
     * Checks for command validity and calls the command executor if valid.
     *
     * @param event The event received to check for a command.
     */
    static void onMessageEvent(MessageCreateEvent event) {
        try {
            if (event.getGuildId().isPresent() && event.getMessage().getContent().isPresent() && !event.getMessage().getContent().get().isEmpty() && event.getMember().isPresent() && !event.getMember().get().isBot()) {
                String content = event.getMessage().getContent().get();
                //Message is a valid guild message (not DM and not from a bot). Check if in correct channel.
                GuildSettings settings = DatabaseManager.getManager().getSettings(event.getGuildId().get());
                if (content.startsWith(settings.getPrefix())) {
                    //Prefixed with ! which should mean it is a command, convert and confirm.
                    String[] argsOr = content.split("\\s+");
                    if (argsOr.length > 1) {
                        ArrayList<String> argsOr2 = new ArrayList<>(Arrays.asList(argsOr).subList(1, argsOr.length));
                        String[] args = argsOr2.toArray(new String[argsOr2.size()]);

                        String command = argsOr[0].replace(settings.getPrefix(), "");
                        CommandExecutor.getExecutor().issueCommand(command, args, event, settings);
                    } else if (argsOr.length == 1) {
                        //Only command... no args.
                        CommandExecutor.getExecutor().issueCommand(argsOr[0].replace(settings.getPrefix(), ""), new String[0], event, settings);
                    }
                } else if (!event.getMessage().mentionsEveryone() && !content.contains("@here") && (content.startsWith("<@" + Main.getClient().getSelfId().get().asString() + ">") || content.startsWith("<@!" + Main.getClient().getSelfId().get().asString() + ">"))) {
                    String[] argsOr = content.split("\\s+");
                    if (argsOr.length > 2) {
                        ArrayList<String> argsOr2 = new ArrayList<>(Arrays.asList(argsOr).subList(2, argsOr.length));
                        String[] args = argsOr2.toArray(new String[argsOr2.size()]);

                        String command = argsOr[1];
                        CommandExecutor.getExecutor().issueCommand(command, args, event, settings);
                    } else if (argsOr.length == 2) {
                        //No args...
                        CommandExecutor.getExecutor().issueCommand(argsOr[1], new String[0], event, settings);
                    } else if (argsOr.length == 1) {
                        //Only TicketBird mentioned...
                        CommandExecutor.getExecutor().issueCommand("TicketBird", new String[0], event, settings);
                    }
                } else {
                    //Alright, let's check if its a ticket since its not a command (also prevents oddities with command handling)
                    MessageCreateListener.onMessageCreate(event, settings);
                }
            }
        } catch (Exception e) {
            Logger.getLogger().exception(event.getMember().get(), "Command error; event message: " + event.getMessage().getContent().get(), e, true, CommandListener.class);
        }
    }
}