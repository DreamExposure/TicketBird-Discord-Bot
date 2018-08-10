package org.dreamexposure.ticketbird.module.command;

import org.dreamexposure.ticketbird.Main;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import org.dreamexposure.ticketbird.utils.GeneralUtils;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

import java.util.ArrayList;

public class CommandExecutor {
    private static CommandExecutor instance;
    private final ArrayList<ICommand> commands = new ArrayList<>();

    private CommandExecutor() {
    }

    /**
     * Gets the instance of the CommandExecutor.
     *
     * @return The instance of the CommandExecutor.
     */
    public static CommandExecutor getExecutor() {
        if (instance == null) {
            instance = new CommandExecutor();
        }
        return instance;
    }

    /**
     * Enables the CommandExecutor and sets up the Listener.
     *
     * @return The CommandExecutor's instance.
     */
    public CommandExecutor enable() {
        EventDispatcher dispatcher = Main.getClient().getDispatcher();
        dispatcher.registerListener(new CommandListener(this));
        return instance;
    }

    //Functional

    /**
     * Registers a command that can be executed.
     *
     * @param _command The command to register.
     */
    public void registerCommand(ICommand _command) {
        commands.add(_command);
    }

    /**
     * Issues a command if valid, else does nothing.
     *
     * @param cmd    The Command to issue.
     * @param argsOr The command arguments used.
     * @param event  The Event received.
     */
    void issueCommand(String cmd, String[] argsOr, MessageReceivedEvent event, GuildSettings settings) {

        String[] args;
        if (argsOr.length > 0) {
            String toParse = GeneralUtils.getContent(argsOr, 0);
            args = GeneralUtils.overkillParser(toParse).split(" ");
        } else {
            args = new String[0];
        }

        for (ICommand c : commands) {
            if (c.getCommand().equalsIgnoreCase(cmd) || c.getAliases().contains(cmd.toLowerCase())) {
                c.issueCommand(args, event, settings);
            }
        }

    }

    /**
     * Gets an ArrayList of all valid commands.
     *
     * @return An ArrayList of all valid commands.
     */
    ArrayList<String> getAllCommands() {
        ArrayList<String> cmds = new ArrayList<>();
        for (ICommand c : commands) {
            if (!cmds.contains(c.getCommand())) {
                cmds.add(c.getCommand());
            }
        }
        return cmds;
    }

    ArrayList<ICommand> getCommands() {
        return commands;
    }

    ICommand getCommand(String cmdNameOrAlias) {
        for (ICommand c : commands) {
            if (c.getCommand().equalsIgnoreCase(cmdNameOrAlias) || c.getAliases().contains(cmdNameOrAlias.toLowerCase())) {
                return c;
            }
        }
        return null;
    }
}