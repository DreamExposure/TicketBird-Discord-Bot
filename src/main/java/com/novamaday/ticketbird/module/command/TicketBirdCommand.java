package com.novamaday.ticketbird.module.command;

import com.novamaday.ticketbird.Main;
import com.novamaday.ticketbird.database.DatabaseManager;
import com.novamaday.ticketbird.message.ChannelManager;
import com.novamaday.ticketbird.message.MessageManager;
import com.novamaday.ticketbird.objects.command.CommandInfo;
import com.novamaday.ticketbird.objects.guild.GuildSettings;
import com.novamaday.ticketbird.utils.GeneralUtils;
import com.novamaday.ticketbird.utils.GlobalVars;
import com.novamaday.ticketbird.utils.UserUtils;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.EmbedBuilder;

import java.util.ArrayList;
import java.util.EnumSet;

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
        CommandInfo info = new CommandInfo("TicketBird");
        info.setDescription("Used to configure TicketBird");
        info.setExample("=TicketBird (function) (value)");

        info.getSubCommands().put("setup", "Starts the initial bot setup/installation.");
        info.getSubCommands().put("staff", "Adds/Removes staff members (for ticket management permissions).");

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
                case "setup":
                    moduleSetup(event, settings);
                    break;
                case "staff":
                    moduleStaff(args, event, settings);
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
        em.appendField("Total Tickets", DatabaseManager.getManager().getTotalTicketCount() + "", true);
        em.appendField(MessageManager.getMessage("Embed.TicketBird.Info.Ping", "%shard%", (guild.getShard().getInfo()[0] + 1) + "/" + Main.getClient().getShardCount(), settings), guild.getShard().getResponseTime() + "ms", false);
        em.withFooterText(MessageManager.getMessage("Embed.TicketBird.Info.Patron", settings) + ": https://www.patreon.com/Novafox");
        em.withUrl("https://ticketbird.novamaday.com");
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
        String INVITE_LINK = "https://discord.gg/2TFqyuy";
        MessageManager.sendMessage(MessageManager.getMessage("TicketBird.InviteLink", "%link%", INVITE_LINK, settings), event);
    }

    private void moduleSetup(MessageReceivedEvent event, GuildSettings settings) {
        if (event.getGuild().getCategoryByID(settings.getCloseCategory()) == null) {
            //Do initial setup!
            MessageManager.sendMessage(MessageManager.getMessage("Setup.Working", settings), event);

            //Create categories...
            try {
                settings.setAwaitingCategory(ChannelManager.createCategory("Tickets Awaiting Response", event.getGuild()).getLongID());
                settings.setRespondedCategory(ChannelManager.createCategory("Tickets Responded To", event.getGuild()).getLongID());
                settings.setHoldCategory(ChannelManager.createCategory("Tickets On Hold", event.getGuild()).getLongID());
                settings.setCloseCategory(ChannelManager.createCategory("Tickets Closed", event.getGuild()).getLongID());

                settings.setSupportChannel(ChannelManager.createChannel("support-request", event.getGuild()).getLongID());

                IChannel support = event.getGuild().getChannelByID(settings.getSupportChannel());

                //Set channel permissions...
                EnumSet<Permissions> toAdd = EnumSet.noneOf(Permissions.class);
                toAdd.add(Permissions.SEND_MESSAGES);
                toAdd.add(Permissions.READ_MESSAGES);
                toAdd.add(Permissions.READ_MESSAGE_HISTORY);

                support.overrideRolePermissions(event.getGuild().getEveryoneRole(), toAdd, EnumSet.noneOf(Permissions.class));

                support.changeTopic(MessageManager.getMessage("Support.DefaultTopic", settings));
                IMessage staticMsg = MessageManager.sendMessage(GeneralUtils.getNormalStaticSupportMessage(event.getGuild(), settings), support);

                settings.setStaticMessage(staticMsg.getLongID());

                //Update database
                DatabaseManager.getManager().updateSettings(settings);

                MessageManager.sendMessage(MessageManager.getMessage("Setup.Complete", settings), event);
            } catch (NullPointerException e) {
                MessageManager.sendMessage(MessageManager.getMessage("Notification.Perm.Bot", settings), event);
            }

        } else {
            //Setup has already been done.
            MessageManager.sendMessage(MessageManager.getMessage("Setup.Already", settings), event);
        }
    }

    private void moduleStaff(String[] args, MessageReceivedEvent event, GuildSettings settings) {
        if (args.length == 3) {
            String name = args[2];
            IUser userFromName = event.getGuild().getUserByID(UserUtils.getUser(name, event.getGuild()));

            if (userFromName == null) {
                MessageManager.sendMessage(MessageManager.getMessage("Notification.User.NotFound", settings), event);
                return;
            }

            switch (args[1].toLowerCase()) {
                case "add":
                    if (settings.getStaff().contains(userFromName.getLongID())) {
                        MessageManager.sendMessage(MessageManager.getMessage("Staff.Add.Already", settings), event);
                    } else {
                        settings.getStaff().add(userFromName.getLongID());
                        DatabaseManager.getManager().updateSettings(settings);
                        MessageManager.sendMessage(MessageManager.getMessage("Staff.Add.Success", settings), event);
                    }
                    break;
                case "remove":
                    if (settings.getStaff().contains(userFromName.getLongID())) {
                        settings.getStaff().remove(userFromName.getLongID());
                        DatabaseManager.getManager().updateSettings(settings);
                        MessageManager.sendMessage(MessageManager.getMessage("Staff.Remove.Success", settings), event);
                    } else {
                        MessageManager.sendMessage(MessageManager.getMessage("Staff.Remove.Not", settings), event);
                    }
                    break;
                default:
                    MessageManager.sendMessage(MessageManager.getMessage("Staff.Specify", settings), event);
                    break;
            }
        } else {
            MessageManager.sendMessage(MessageManager.getMessage("Staff.Specify", settings), event);
        }
    }
}