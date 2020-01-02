package org.dreamexposure.ticketbird.module.command;

import org.dreamexposure.ticketbird.Main;
import org.dreamexposure.ticketbird.database.DatabaseManager;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.message.ChannelManager;
import org.dreamexposure.ticketbird.message.MessageManager;
import org.dreamexposure.ticketbird.objects.bot.BotSettings;
import org.dreamexposure.ticketbird.objects.command.CommandInfo;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import org.dreamexposure.ticketbird.utils.GeneralUtils;
import org.dreamexposure.ticketbird.utils.GlobalVars;
import org.dreamexposure.ticketbird.utils.UserUtils;

import java.util.ArrayList;
import java.util.function.Consumer;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

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
    public boolean issueCommand(String[] args, MessageCreateEvent event, GuildSettings settings) {
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
                    MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Args.Invalid", settings), event);
                    break;
            }
        }
        return false;
    }

    private void moduleTicketBirdInfo(MessageCreateEvent event, GuildSettings settings) {
        Consumer<EmbedCreateSpec> embed = spec -> {
            spec.setAuthor("TicketBird", GlobalVars.siteUrl, GlobalVars.iconUrl);
            spec.setTitle(MessageManager.getMessage("Embed.TicketBird.Info.Title", settings));
            spec.addField(MessageManager.getMessage("Embed.TicketBird.Info.Developer", settings), "DreamExposure", true);
            spec.addField(MessageManager.getMessage("Embed.TicketBird.Info.Version", settings), GlobalVars.version, true);
            spec.addField(MessageManager.getMessage("Embed.TicketBird.Info.Library", settings), "Discord4J, version " + GlobalVars.d4jVersion, true);
            spec.addField("Shard Index", BotSettings.SHARD_INDEX.get() + "/" + BotSettings.SHARD_COUNT.get(), true);
            spec.addField(MessageManager.getMessage("Embed.TicketBird.Info.TotalGuilds", settings), Main.getClient().getGuilds().count().block() + "", true);
            spec.addField("Total Tickets", DatabaseManager.getManager().getTotalTicketCount() + "", true);
            spec.setFooter(MessageManager.getMessage("Embed.TicketBird.Info.Patron", settings) + ": https://www.patreon.com/Novafox", null);
            spec.setUrl(GlobalVars.siteUrl);
            spec.setColor(GlobalVars.embedColor);
        };
        MessageManager.sendMessageAsync(embed, event);
    }

    private void moduleSettings(MessageCreateEvent event, GuildSettings settings) {
        Consumer<EmbedCreateSpec> embed = spec -> {
            spec.setAuthor("TicketBird", GlobalVars.siteUrl, GlobalVars.iconUrl);
            spec.setTitle(MessageManager.getMessage("Embed.TicketBird.Settings.Title", settings));
            spec.addField(MessageManager.getMessage("Embed.TicketBird.Settings.Patron", settings), String.valueOf(settings.isPatronGuild()), true);
            spec.addField(MessageManager.getMessage("Embed.TicketBird.Settings.Dev", settings), String.valueOf(settings.isDevGuild()), true);
            spec.addField(MessageManager.getMessage("Embed.TicketBird.Settings.Language", settings), settings.getLang(), true);
            spec.addField(MessageManager.getMessage("Embed.TicketBird.Settings.Prefix", settings), settings.getPrefix(), true);
            if (settings.isUseProjects())
                spec.addField("Use Projects", "Enabled", true); //TODO: Add translations
            else
                spec.addField("Use Projects", "Disabled", true);
            spec.setFooter(MessageManager.getMessage("Embed.TicketBird.Info.Patron", settings) + ": https://www.patreon.com/Novafox", null);
            spec.setUrl(GlobalVars.siteUrl);
            spec.setColor(GlobalVars.embedColor);
        };
        MessageManager.sendMessageAsync(embed, event);
    }

    private void modulePrefix(String[] args, MessageCreateEvent event, GuildSettings settings) {
        if (args.length == 2) {
            String prefix = args[1];

            settings.setPrefix(prefix);
            DatabaseManager.getManager().updateSettings(settings);

            MessageManager.sendMessageAsync(MessageManager.getMessage("TicketBird.Prefix.Set", "%prefix%", prefix, settings), event);
        } else {
            MessageManager.sendMessageAsync(MessageManager.getMessage("TicketBird.Prefix.Specify", settings), event);
        }
    }

    private void moduleLanguage(String[] args, MessageCreateEvent event, GuildSettings settings) {
        if (args.length == 2) {
            String value = args[1];
            if (MessageManager.isSupported(value)) {
                String valid = MessageManager.getValidLang(value);

                settings.setLang(valid);
                DatabaseManager.getManager().updateSettings(settings);

                MessageManager.sendMessageAsync(MessageManager.getMessage("TicketBird.Lang.Success", settings), event);
            } else {
                String langs = MessageManager.getLangs().toString().replace("[", "").replace("]", "");
                MessageManager.sendMessageAsync(MessageManager.getMessage("TicketBird.Lang.Unsupported", "%values%", langs, settings), event);
            }
        } else {
            String langs = MessageManager.getLangs().toString().replace("[", "").replace("]", "");
            MessageManager.sendMessageAsync(MessageManager.getMessage("TicketBird.Lang.Specify", "%values%", langs, settings), event);
        }
    }

    private void moduleInvite(MessageCreateEvent event, GuildSettings settings) {
        String INVITE_LINK = "https://discord.gg/2TFqyuy";
        MessageManager.sendMessageAsync(MessageManager.getMessage("TicketBird.InviteLink", "%link%", INVITE_LINK, settings), event);
    }

    @SuppressWarnings("ConstantConditions")
    private void moduleSetup(MessageCreateEvent event, GuildSettings settings) {
        Guild guild = event.getGuild().block();

        if (settings.getCloseCategory() == null || guild.getChannelById(settings.getCloseCategory()).onErrorResume(e -> Mono.empty()).block() == null) {
            //Do initial setup!
            MessageManager.sendMessageAsync(MessageManager.getMessage("Setup.Working", settings), event);

            //Create categories...
            try {
                settings.setAwaitingCategory(ChannelManager.createCategory("Tickets Awaiting Response", guild).getId());
                settings.setRespondedCategory(ChannelManager.createCategory("Tickets Responded To", guild).getId());
                settings.setHoldCategory(ChannelManager.createCategory("Tickets On Hold", guild).getId());
                settings.setCloseCategory(ChannelManager.createCategory("Tickets Closed", guild).getId());

                TextChannel support = ChannelManager.createChannel("support-request", MessageManager.getMessage("Support.DefaultTopic", settings), null, guild);
                settings.setSupportChannel(support.getId());

                //Set channel permissions...
                PermissionSet toAdd = PermissionSet.of(Permission.SEND_MESSAGES, Permission.VIEW_CHANNEL, Permission.READ_MESSAGE_HISTORY);

                PermissionOverwrite forEveryone = PermissionOverwrite.forRole(guild.getEveryoneRole().block().getId(), toAdd, PermissionSet.none());

                support.addRoleOverwrite(guild.getEveryoneRole().block().getId(), forEveryone).subscribe();

                Message staticMsg = MessageManager.sendMessageSync(GeneralUtils.getNormalStaticSupportMessage(guild, settings), support);

                settings.setStaticMessage(staticMsg.getId());

                //Update database
                DatabaseManager.getManager().updateSettings(settings);

                MessageManager.sendMessageAsync(MessageManager.getMessage("Setup.Complete", settings), event);
            } catch (Exception e) {
                //noinspection OptionalGetWithoutIsPresent
                Logger.getLogger().exception(event.getMember().get(), "Setup Failed", e, true, getClass());
                MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Perm.Bot", settings), event);
            }

        } else {
            //Setup has already been done.
            MessageManager.sendMessageAsync(MessageManager.getMessage("Setup.Already", settings), event);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void moduleStaff(String[] args, MessageCreateEvent event, GuildSettings settings) {
        if (args.length == 3) {
            String name = args[2];
            Member userFromName = event.getGuild().block().getMemberById(UserUtils.getUser(name, event.getGuild().block())).onErrorResume(e -> Mono.empty()).block();

            if (userFromName == null) {
                MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.User.NotFound", settings), event);
                return;
            }

            switch (args[1].toLowerCase()) {
                case "add":
                    if (settings.getStaff().contains(userFromName.getId())) {
                        MessageManager.sendMessageAsync(MessageManager.getMessage("Staff.Add.Already", settings), event);
                    } else {
                        settings.getStaff().add(userFromName.getId());
                        DatabaseManager.getManager().updateSettings(settings);
                        MessageManager.sendMessageAsync(MessageManager.getMessage("Staff.Add.Success", settings), event);
                    }
                    break;
                case "remove":
                    if (settings.getStaff().contains(userFromName.getId())) {
                        settings.getStaff().remove(userFromName.getId());
                        DatabaseManager.getManager().updateSettings(settings);
                        MessageManager.sendMessageAsync(MessageManager.getMessage("Staff.Remove.Success", settings), event);
                    } else {
                        MessageManager.sendMessageAsync(MessageManager.getMessage("Staff.Remove.Not", settings), event);
                    }
                    break;
                default:
                    MessageManager.sendMessageAsync(MessageManager.getMessage("Staff.Specify", settings), event);
                    break;
            }
        } else {
            MessageManager.sendMessageAsync(MessageManager.getMessage("Staff.Specify", settings), event);
        }
    }
}
