package org.dreamexposure.ticketbird.module.command;

import org.dreamexposure.ticketbird.message.MessageManager;
import org.dreamexposure.ticketbird.objects.command.CommandInfo;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import org.dreamexposure.ticketbird.utils.GlobalVars;

import java.util.ArrayList;
import java.util.function.Consumer;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;

public class HelpCommand implements ICommand {
    /**
     * Gets the command this Object is responsible for.
     *
     * @return The command this Object is responsible for.
     */
    @Override
    public String getCommand() {
        return "help";
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
        CommandInfo info = new CommandInfo("help");
        info.setDescription("Displays help (duh).");
        info.setExample("=help (command) (sub-command)");

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
        if (args.length < 1) {
            Consumer<EmbedCreateSpec> embed = spec -> {
                spec.setAuthor("TicketBird", GlobalVars.siteUrl, GlobalVars.iconUrl);
                spec.setTitle("TicketBird Command Help");
                for (ICommand c : CommandExecutor.getExecutor().getCommands()) {
                    if (c.getAliases().size() > 0) {
                        String al = c.getAliases().toString();
                        spec.addField(c.getCommand() + " " + al, c.getCommandInfo().getDescription(), true);
                    } else {
                        spec.addField(c.getCommand(), c.getCommandInfo().getDescription(), true);
                    }
                }
                spec.setFooter("Check out the official site for more command info!", null);
                spec.setUrl("https://ticketbird.dreamexposure.org/commands");
                spec.setColor(GlobalVars.embedColor);
            };
            MessageManager.sendMessageAsync(embed, event);
        } else if (args.length == 1) {
            String cmdFor = args[0];
            ICommand cmd = CommandExecutor.getExecutor().getCommand(cmdFor);
            if (cmd != null) {
                MessageManager.sendMessageAsync(getCommandInfoEmbed(cmd), event);
            }
        } else if (args.length == 2) {
            //Display sub command info
            String cmdFor = args[0];
            ICommand cmd = CommandExecutor.getExecutor().getCommand(cmdFor);
            if (cmd != null) {
                if (cmd.getCommandInfo().getSubCommands().containsKey(args[1].toLowerCase())) {
                    MessageManager.sendMessageAsync(getSubCommandEmbed(cmd, args[1].toLowerCase()), event);
                } else {
                    //Sub command does not exist.
                    MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Args.InvalidSubCmd", settings), event);
                }
            }
        }

        return false;
    }

    //Embed formatters
    private Consumer<EmbedCreateSpec> getCommandInfoEmbed(ICommand cmd) {
        return spec -> {
            spec.setAuthor("TicketBird", GlobalVars.siteUrl, GlobalVars.iconUrl);
            spec.addField("Command", cmd.getCommand(), true);
            spec.addField("Description", cmd.getCommandInfo().getDescription(), true);
            spec.addField("Example", cmd.getCommandInfo().getExample(), true);

            //Loop through sub commands
            if (cmd.getCommandInfo().getSubCommands().size() > 0) {
                String subs = cmd.getCommandInfo().getSubCommands().keySet().toString();
                subs = subs.replace("[", "").replace("]", "");
                spec.addField("Sub-Commands", subs, false);
            }

            spec.setFooter("<> = required | () = optional", null);

            spec.setUrl("https://ticketbird.dreamexposure.org/commands");

            spec.setColor(GlobalVars.embedColor);

        };
    }

    private Consumer<EmbedCreateSpec> getSubCommandEmbed(ICommand cmd, String subCommand) {
        return spec -> {
            spec.setAuthor("TicketBird", GlobalVars.siteUrl, GlobalVars.iconUrl);
            spec.addField("Command", cmd.getCommand(), true);
            spec.addField("Sub Command", subCommand, true);

            spec.addField("Usage", cmd.getCommandInfo().getSubCommands().get(subCommand), false);

            spec.setFooter("<> = required | () = optional", null);

            spec.setUrl("https://ticketbird.dreamexposure.org/commands");

            spec.setColor(GlobalVars.embedColor);
        };
    }
}
