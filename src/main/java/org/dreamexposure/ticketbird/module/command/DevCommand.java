package org.dreamexposure.ticketbird.module.command;


import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.dreamexposure.ticketbird.database.DatabaseManager;
import org.dreamexposure.ticketbird.message.MessageManager;
import org.dreamexposure.ticketbird.objects.command.CommandInfo;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import org.dreamexposure.ticketbird.utils.GlobalVars;

import java.util.ArrayList;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class DevCommand implements ICommand {

    /**
     * Gets the command this Object is responsible for.
     *
     * @return The command this Object is responsible for.
     */
    @Override
    public String getCommand() {
        return "dev";
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
        CommandInfo ci = new CommandInfo("dev");
        ci.setDescription("Used for developer commands. Only able to be used by registered developers");
        ci.setExample("=dev <function> (value)");
        ci.getSubCommands().put("patron", "Sets a guild as a patron.");
        ci.getSubCommands().put("dev", "Sets a guild as a test/dev guild.");
        ci.getSubCommands().put("reloadlangs", "Reloads the lang files for changes.");

        return ci;
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
        if (event.getMember().get().getId().equals(GlobalVars.novaId)) {
            if (args.length < 1) {
                MessageManager.sendMessageAsync("Please specify the function you would like to execute. To view valid functions use `!help dev`", event);
            } else {
                switch (args[0].toLowerCase()) {
                    case "patron" -> modulePatron(args, event);
                    case "dev" -> moduleDevGuild(args, event);
                    case "reloadlangs" -> moduleReloadLangs(event);
                    default -> MessageManager.sendMessageAsync("Invalid sub command! Use `!help dev` to view valid sub commands!", event);
                }
            }
        } else {
            MessageManager.sendMessageAsync("You are not a registered TicketBird developer! If this is a mistake please contact Nova!", event);
        }
        return false;
    }

    private void modulePatron(String[] args, MessageCreateEvent event) {
        if (args.length == 2) {
            Snowflake guildId = Snowflake.of(args[1]);
            if (event.getClient().getGuildById(guildId).block() != null) {
                GuildSettings settings = DatabaseManager.getManager().getSettings(guildId);
                settings.setPatronGuild(!settings.isPatronGuild());

                boolean isPatron = settings.isPatronGuild();

                DatabaseManager.getManager().updateSettings(settings);

                MessageManager.sendMessageAsync("Guild with ID: `" + guildId + "` is patron set to: `" + isPatron + "`", event);
            } else {
                MessageManager.sendMessageAsync("Guild not found or is not connected to TicketBird!", event);
            }
        } else {
            MessageManager.sendMessageAsync("Please specify the ID of the guild to set as a patron guild with `!dev patron <ID>`", event);
        }
    }

    private void moduleDevGuild(String[] args, MessageCreateEvent event) {
        if (args.length == 2) {
            Snowflake guildId = Snowflake.of(args[1]);
            if (event.getClient().getGuildById(guildId).block() != null) {
                GuildSettings settings = DatabaseManager.getManager().getSettings(guildId);
                settings.setDevGuild(!settings.isDevGuild());

                boolean isPatron = settings.isDevGuild();

                DatabaseManager.getManager().updateSettings(settings);

                MessageManager.sendMessageAsync("Guild with ID: `" + guildId + "` is dev guild set to: `" + isPatron + "`", event);
            } else {
                MessageManager.sendMessageAsync("Guild not found or is not connected to TicketBird!", event);
            }
        } else {
            MessageManager.sendMessageAsync("Please specify the ID of the guild to set as a dev guild with `!dev dev <ID>`", event);
        }
    }

    private void moduleReloadLangs(MessageCreateEvent event) {
        MessageManager.sendMessageAsync("Reloading lang files!", event);

        MessageManager.reloadLangs();

        MessageManager.sendMessageAsync("All lang files reloaded!", event);
    }
}
