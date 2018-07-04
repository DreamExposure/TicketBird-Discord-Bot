package com.novamaday.ticketbird.module.command;

import com.novamaday.ticketbird.Main;
import com.novamaday.ticketbird.crypto.KeyGenerator;
import com.novamaday.ticketbird.database.DatabaseManager;
import com.novamaday.ticketbird.logger.Logger;
import com.novamaday.ticketbird.message.MessageManager;
import com.novamaday.ticketbird.objects.api.UserAPIAccount;
import com.novamaday.ticketbird.objects.command.CommandInfo;
import com.novamaday.ticketbird.objects.guild.GuildSettings;
import com.novamaday.ticketbird.utils.GlobalVars;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.IShard;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.ArrayList;

public class DevCommand implements ICommand {

    private ScriptEngine factory = new ScriptEngineManager().getEngineByName("nashorn");

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
        ci.getSubCommands().put("leave", "Leaves the specified guild.");
        ci.getSubCommands().put("reloadlangs", "Reloads the lang files for changes.");
        ci.getSubCommands().put("reload", "Logs out and then logs in every shard.");
        ci.getSubCommands().put("eval", "Evaluates the given code.");
        ci.getSubCommands().put("testShards", "Tests to make sure all shards respond.");

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
    public Boolean issueCommand(String[] args, MessageReceivedEvent event, GuildSettings settings) {
        if (event.getAuthor().getLongID() == GlobalVars.novaId) {
            if (args.length < 1) {
                MessageManager.sendMessage("Please specify the function you would like to execute. To view valid functions use `!help dev`", event);
            } else {
                switch (args[0].toLowerCase()) {
                    case "patron":
                        modulePatron(args, event);
                        break;
                    case "dev":
                        moduleDevGuild(args, event);
                        break;
                    case "leave":
                        moduleLeaveGuild(args, event);
                        break;
                    case "reloadlangs":
                        moduleReloadLangs(event);
                        break;
                    case "reload":
                        moduleReload(event);
                        break;
                    case "eval":
                        moduleEval(event);
                        break;
                    case "testshards":
                        moduleTestShards(event);
                        break;
                    case "api-register":
                        registerApiKey(args, event);
                        break;
                    case "api-block":
                        blockAPIKey(args, event);
                        break;
                    default:
                        MessageManager.sendMessage("Invalid sub command! Use `!help dev` to view valid sub commands!", event);
                        break;
                }
            }
        } else {
            MessageManager.sendMessage("You are not a registered TicketBird developer! If this is a mistake please contact Nova!", event);
        }
        return false;
    }

    private void modulePatron(String[] args, MessageReceivedEvent event) {
        if (args.length == 2) {
            long guildId = Long.valueOf(args[1]);
            if (Main.getClient().getGuildByID(guildId) != null) {
                GuildSettings settings = DatabaseManager.getManager().getSettings(guildId);
                settings.setPatronGuild(!settings.isPatronGuild());

                boolean isPatron = settings.isPatronGuild();

                DatabaseManager.getManager().updateSettings(settings);

                MessageManager.sendMessage("Guild with ID: `" + guildId + "` is patron set to: `" + isPatron + "`", event);
            } else {
                MessageManager.sendMessage("Guild not found or is not connected to TicketBird!", event);
            }
        } else {
            MessageManager.sendMessage("Please specify the ID of the guild to set as a patron guild with `!dev patron <ID>`", event);
        }
    }

    @SuppressWarnings("all")
    private void moduleEval(MessageReceivedEvent event) {
        IGuild guild = event.getGuild();
        IUser user = event.getAuthor();
        IMessage message = event.getMessage();
        IDiscordClient client = event.getClient();
        IChannel channel = event.getChannel();
        String input = message.getContent().substring(message.getContent().indexOf("eval") + 5).replaceAll("`", "");
        Object o = null;
        factory.put("guild", guild);
        factory.put("channel", channel);
        factory.put("user", user);
        factory.put("message", message);
        factory.put("command", this);
        factory.put("client", client);
        factory.put("builder", new EmbedBuilder());
        factory.put("cUser", client.getOurUser());

        try {
            o = factory.eval(input);
        } catch (Exception ex) {
            EmbedBuilder em = new EmbedBuilder();
            em.withAuthorIcon(guild.getIconURL());
            em.withAuthorName("Error");
            em.withDesc(ex.getMessage());
            em.withFooterText("Eval failed");
            em.withColor(GlobalVars.embedColor);
            MessageManager.sendMessage(em.build(), channel);
            return;
        }

        EmbedBuilder em = new EmbedBuilder();
        em.withAuthorIcon(guild.getIconURL());
        em.withAuthorName("Success!");
        em.withColor(GlobalVars.embedColor);
        em.withTitle("Evaluation output.");
        em.withDesc(o == null ? "No output, object is null" : o.toString());
        em.appendField("Input", "```java\n" + input + "\n```", false);
        em.withFooterText("Eval successful!");
        MessageManager.sendMessage(em.build(), channel);
    }

    private void moduleDevGuild(String[] args, MessageReceivedEvent event) {
        if (args.length == 2) {
            long guildId = Long.valueOf(args[1]);
            if (Main.getClient().getGuildByID(guildId) != null) {
                GuildSettings settings = DatabaseManager.getManager().getSettings(guildId);
                settings.setDevGuild(!settings.isDevGuild());

                boolean isPatron = settings.isDevGuild();

                DatabaseManager.getManager().updateSettings(settings);

                MessageManager.sendMessage("Guild with ID: `" + guildId + "` is dev guild set to: `" + isPatron + "`", event);
            } else {
                MessageManager.sendMessage("Guild not found or is not connected to TicketBird!", event);
            }
        } else {
            MessageManager.sendMessage("Please specify the ID of the guild to set as a dev guild with `!dev dev <ID>`", event);
        }
    }

    private void moduleLeaveGuild(String[] args, MessageReceivedEvent event) {
        if (args.length == 2) {
            if (Main.getClient().getGuildByID(Long.valueOf(args[1])) != null) {
                RequestBuffer.request(() -> {
                    try {
                        Main.getClient().getGuildByID(Long.valueOf(args[1])).leave();
                    } catch (DiscordException e) {
                        Logger.getLogger().exception(event.getMessage().getAuthor(), "Failed to leave guild", e, this.getClass());
                    }
                });
                MessageManager.sendMessage("Left Guild!", event);
            } else {
                MessageManager.sendMessage("Guild not found!", event);
            }
        } else {
            MessageManager.sendMessage("Please specify the ID of the guild to leave with `!dev leave <ID>`", event);
        }
    }

    private void moduleReloadLangs(MessageReceivedEvent event) {
        MessageManager.sendMessage("Reloading lang files!", event);

        MessageManager.reloadLangs();

        MessageManager.sendMessage("All lang files reloaded!", event);
    }

    private void moduleReload(MessageReceivedEvent event) {
        IMessage msg = MessageManager.sendMessage("Reloading TicketBird! This may take a moment!", event);

        for (IShard s : msg.getClient().getShards()) {
            s.logout();
            s.login();
        }
        MessageManager.sendMessage("TicketBIrd successfully reloaded!", event);
    }

    private void moduleTestShards(MessageReceivedEvent event) {
        MessageManager.sendMessage("Testing shard responses...", event);

        StringBuilder r = new StringBuilder();
        for (IShard s : Main.getClient().getShards()) {
            r.append(s.getInfo()[0]).append(": ").append(s.isReady()).append("\n");
        }

        MessageManager.sendMessage(r.toString(), event);
    }

    private void registerApiKey(String[] args, MessageReceivedEvent event) {
        if (args.length == 2) {
            MessageManager.sendMessage("Registering new API key...", event);

            String userId = args[1];

            UserAPIAccount account = new UserAPIAccount();
            account.setUserId(userId);
            account.setAPIKey(KeyGenerator.csRandomAlphaNumericString(64));
            account.setTimeIssued(System.currentTimeMillis());
            account.setBlocked(false);
            account.setUses(0);

            if (DatabaseManager.getManager().updateAPIAccount(account)) {
                MessageManager.sendMessage("Check your DMs for the new API Key!", event);
                MessageManager.sendDirectMessage(account.getAPIKey(), event.getAuthor());
            } else {
                MessageManager.sendMessage("Error occurred! Could not register new API key!", event);
            }
        } else {
            MessageManager.sendMessage("Please specify the USER ID linked to the key!", event);
        }
    }

    private void blockAPIKey(String[] args, MessageReceivedEvent event) {
        if (args.length == 2) {
            MessageManager.sendMessage("Blocking API key...", event);

            String key = args[1];

            UserAPIAccount account = DatabaseManager.getManager().getAPIAccount(key);
            account.setBlocked(true);

            if (DatabaseManager.getManager().updateAPIAccount(account))
                MessageManager.sendMessage("Successfully blocked API key!", event);
            else
                MessageManager.sendMessage("Error occurred! Could not block API key!", event);
        } else {
            MessageManager.sendMessage("Please specify the API KEY!", event);
        }
    }
}