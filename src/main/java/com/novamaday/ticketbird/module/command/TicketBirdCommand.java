package com.novamaday.ticketbird.module.command;

import com.novamaday.ticketbird.Main;
import com.novamaday.ticketbird.database.DatabaseManager;
import com.novamaday.ticketbird.message.MessageManager;
import com.novamaday.ticketbird.objects.command.CommandInfo;
import com.novamaday.ticketbird.objects.guild.GuildSettings;
import com.novamaday.ticketbird.utils.GlobalVars;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.util.EmbedBuilder;

import java.util.ArrayList;

public class TicketBirdCommand implements ICommand {

    /**
     * Gets the command this Object is responsible for.
     *
     * @return The command this Object is responsible for.
     */
    @Override
    public String getCommand() {
        return "Ticketbird";
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
        CommandInfo info = new CommandInfo("event");
        info.setDescription("Used to configure TicketBird");
        info.setExample("=TicketBird (function) (value)");

        info.getSubCommands().put("settings", "Displays the bot's settings.");
        info.getSubCommands().put("language", "Sets the bot's language.");
        info.getSubCommands().put("lang", "Sets the bot's language.");
        info.getSubCommands().put("prefix", "Sets the bot's prefix.");
        info.getSubCommands().put("invite", "Displays an invite to the support guild.");

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
        if (args.length < 1) {
            moduleTicketBirdInfo(event, settings);
        } else {
            switch (args[0].toLowerCase()) {
                case "ticketbird":
                    moduleTicketBirdInfo(event, settings);
                    break;
                case "settings":
                    moduleSettings(event, settings);
                    break;
                case "language":
                    moduleLanguage(args, event, settings);
                    break;
                case "lang":
                    moduleLanguage(args, event, settings);
                    break;
                case "prefix":
                    modulePrefix(args, event, settings);
                    break;
                case "invite":
                    moduleInvite(event, settings);
                    break;
                default:
                    MessageManager.sendMessage(MessageManager.getMessage("Notification.Args.Invalid", settings), event);
                    break;
            }
        }
        return false;
    }

    private void moduleTicketBirdInfo(MessageReceivedEvent event, GuildSettings settings) {
        IGuild guild = event.getGuild();

        EmbedBuilder em = new EmbedBuilder();
        em.withAuthorIcon(Main.getClient().getGuildByID(GlobalVars.serverId).getIconURL());
        em.withAuthorName("TicketBird!");
        em.withTitle(MessageManager.getMessage("Embed.TicketBird.Info.Title", settings));
        em.appendField(MessageManager.getMessage("Embed.TicketBird.Info.Developer", settings), "NovaFox161", true);
        em.appendField(MessageManager.getMessage("Embed.TicketBird.Info.Version", settings), GlobalVars.version, true);
        em.appendField(MessageManager.getMessage("Embed.TicketBird.Info.Library", settings), "Discord4J, version 2.9.2", false);
        em.appendField(MessageManager.getMessage("Embed.TicketBird.Info.TotalGuilds", settings), Main.getClient().getGuilds().size() + "", true);
        em.appendField(MessageManager.getMessage("Embed.TicketBird.Info.Ping", "%shard%", (guild.getShard().getInfo()[0] + 1) + "/" + Main.getClient().getShardCount(), settings), guild.getShard().getResponseTime() + "ms", false);
        em.withFooterText(MessageManager.getMessage("Embed.TicketBird.Info.Patron", settings) + ": https://www.patreon.com/Novafox");
        em.withUrl("https://www.novamaday.com/ticketbird/");
        em.withColor(GlobalVars.embedColor);
        MessageManager.sendMessage(em.build(), event);
    }

    private void moduleSettings(MessageReceivedEvent event, GuildSettings settings) {
        EmbedBuilder em = new EmbedBuilder();
        em.withAuthorIcon(Main.getClient().getGuildByID(GlobalVars.serverId).getIconURL());
        em.withAuthorName("TicketBird");
        em.withTitle(MessageManager.getMessage("Embed.TicketBird.Settings.Title", settings));
        em.appendField(MessageManager.getMessage("Embed.TicketBird.Settings.Patron", settings), String.valueOf(settings.isPatronGuild()), true);
        em.appendField(MessageManager.getMessage("Embed.TicketBird.Settings.Dev", settings), String.valueOf(settings.isDevGuild()), true);
        em.appendField(MessageManager.getMessage("Embed.TicketBird.Settings.Language", settings), settings.getLang(), true);
        em.appendField(MessageManager.getMessage("Embed.TicketBird.Settings.Prefix", settings), settings.getPrefix(), true);
        //TODO: Add translations...
        em.withFooterText(MessageManager.getMessage("Embed.TicketBird.Info.Patron", settings) + ": https://www.patreon.com/Novafox");
        em.withUrl("https://www.novamaday.com/ticketbird/");
        em.withColor(GlobalVars.embedColor);
        MessageManager.sendMessage(em.build(), event);
    }

    private void modulePrefix(String[] args, MessageReceivedEvent event, GuildSettings settings) {
        if (args.length == 2) {
            String prefix = args[1];

            settings.setPrefix(prefix);
            DatabaseManager.getManager().updateSettings(settings);

            MessageManager.sendMessage(MessageManager.getMessage("TicketBird.Prefix.Set", "%prefix%", prefix, settings), event);
        } else {
            MessageManager.sendMessage(MessageManager.getMessage("TicketBird.Prefix.Specify", settings), event);
        }
    }

    private void moduleLanguage(String[] args, MessageReceivedEvent event, GuildSettings settings) {
        if (args.length == 2) {
            String value = args[1];
            if (MessageManager.isSupported(value)) {
                String valid = MessageManager.getValidLang(value);

                settings.setLang(valid);
                DatabaseManager.getManager().updateSettings(settings);

                MessageManager.sendMessage(MessageManager.getMessage("TicketBird.Lang.Success", settings), event);
            } else {
                String langs = MessageManager.getLangs().toString().replace("[", "").replace("]", "");
                MessageManager.sendMessage(MessageManager.getMessage("TicketBird.Lang.Unsupported", "%values%", langs, settings), event);
            }
        } else {
            String langs = MessageManager.getLangs().toString().replace("[", "").replace("]", "");
            MessageManager.sendMessage(MessageManager.getMessage("TicketBird.Lang.Specify", "%values%", langs, settings), event);
        }
    }

    private void moduleInvite(MessageReceivedEvent event, GuildSettings settings) {
        String INVITE_LINK = "https://discord.gg/AmAMGeN";
        MessageManager.sendMessage(MessageManager.getMessage("TicketBird.InviteLink", "%link%", INVITE_LINK, settings), event);
    }
}