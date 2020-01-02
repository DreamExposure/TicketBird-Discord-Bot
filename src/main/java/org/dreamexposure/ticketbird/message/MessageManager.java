package org.dreamexposure.ticketbird.message;

import org.dreamexposure.ticketbird.file.ReadFile;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import org.dreamexposure.ticketbird.utils.GlobalVars;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "unused"})
public class MessageManager {
    private static JSONObject langs;

    //Lang handling
    public static boolean reloadLangs() {
        try {
            langs = ReadFile.readAllLangFiles();
            return true;
        } catch (Exception e) {
            Logger.getLogger().exception(null, "Failed to reload lang files!", e, true, MessageManager.class);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static List<String> getLangs() {

        return new ArrayList<String>(langs.keySet());
    }

    public static boolean isSupported(String _value) {
        JSONArray names = langs.names();
        for (int i = 0; i < names.length(); i++) {
            if (_value.equalsIgnoreCase(names.getString(i)))
                return true;
        }
        return false;
    }

    public static String getValidLang(String _value) {
        JSONArray names = langs.names();
        for (int i = 0; i < names.length(); i++) {
            if (_value.equalsIgnoreCase(names.getString(i)))
                return names.getString(i);
        }
        return "ENGLISH";
    }

    public static String getMessage(String key, GuildSettings settings) {
        JSONObject messages;

        if (settings.getLang() != null && langs.has(settings.getLang()))
            messages = langs.getJSONObject(settings.getLang());
        else
            messages = langs.getJSONObject("ENGLISH");

        if (messages.has(key))
            return messages.getString(key).replace("%lb%", GlobalVars.lineBreak);
        else
            return "***FAILSAFE MESSAGE*** MESSAGE NOT FOUND!! Message requested: " + key;
    }

    public static String getMessage(String key, String var, String replace, GuildSettings settings) {
        JSONObject messages;

        if (settings.getLang() != null && langs.has(settings.getLang()))
            messages = langs.getJSONObject(settings.getLang());
        else
            messages = langs.getJSONObject("ENGLISH");

        if (messages.has(key))
            return messages.getString(key).replace(var, replace).replace("%lb%", GlobalVars.lineBreak);
        else
            return "***FAILSAFE MESSAGE*** MESSAGE NOT FOUND!! Message requested: " + key;
    }

    //Message sending
    public static void sendMessageAsync(String message, TextChannel channel) {
        channel.createMessage(spec -> spec.setContent(message)).subscribe();
    }

    public static void sendMessageAsync(Consumer<EmbedCreateSpec> embed, TextChannel channel) {
        channel.createMessage(spec -> spec.setEmbed(embed)).subscribe();
    }

    public static void sendMessageAsync(String message, Consumer<EmbedCreateSpec> embed, TextChannel channel) {
        channel.createMessage(spec -> spec.setContent(message).setEmbed(embed)).subscribe();
    }

    public static void sendMessageAsync(String message, MessageCreateEvent event) {
        event.getMessage().getChannel().flatMap(c -> c.createMessage(spec -> spec.setContent(message))).subscribe();
    }

    public static void sendMessageAsync(Consumer<EmbedCreateSpec> embed, MessageCreateEvent event) {
        event.getMessage().getChannel().flatMap(c -> c.createMessage(spec -> spec.setEmbed(embed))).subscribe();
    }

    public static void sendMessageAsync(String message, Consumer<EmbedCreateSpec> embed, MessageCreateEvent event) {
        event.getMessage().getChannel().flatMap(c -> c.createMessage(spec -> spec.setContent(message).setEmbed(embed))).subscribe();
    }

    public static Message sendMessageSync(String message, MessageCreateEvent event) {
        return event.getMessage().getChannel().flatMap(c -> c.createMessage(spec -> spec.setContent(message))).block();
    }

    public static Message sendMessageSync(Consumer<EmbedCreateSpec> embed, MessageCreateEvent event) {
        return event.getMessage().getChannel().flatMap(c -> c.createMessage(spec -> spec.setEmbed(embed))).block();
    }

    public static Message sendMessageSync(String message, Consumer<EmbedCreateSpec> embed, MessageCreateEvent event) {
        return event.getMessage().getChannel().flatMap(c -> c.createMessage(spec -> spec.setContent(message).setEmbed(embed))).block();
    }

    public static Message sendMessageSync(String message, TextChannel channel) {
        return channel.createMessage(spec -> spec.setContent(message)).block();
    }

    public static Message sendMessageSync(Consumer<EmbedCreateSpec> embed, TextChannel channel) {
        return channel.createMessage(spec -> spec.setEmbed(embed)).block();
    }

    public static Message sendMessageSync(String message, Consumer<EmbedCreateSpec> embed, TextChannel channel) {
        return channel.createMessage(spec -> spec.setContent(message).setEmbed(embed)).block();
    }

    public static void sendDirectMessageAsync(String message, User user) {
        user.getPrivateChannel().flatMap(c -> c.createMessage(spec -> spec.setContent(message))).subscribe();
    }

    public static void sendDirectMessageAsync(Consumer<EmbedCreateSpec> embed, User user) {
        user.getPrivateChannel().flatMap(c -> c.createMessage(spec -> spec.setEmbed(embed))).subscribe();
    }

    public static void sendDirectMessageAsync(String message, Consumer<EmbedCreateSpec> embed, User user) {
        user.getPrivateChannel().flatMap(c -> c.createMessage(spec -> spec.setContent(message).setEmbed(embed))).subscribe();
    }

    public static Message sendDirectMessageSync(String message, User user) {
        return user.getPrivateChannel().flatMap(c -> c.createMessage(spec -> spec.setContent(message))).block();
    }

    public static Message sendDirectMessageSync(Consumer<EmbedCreateSpec> embed, User user) {
        return user.getPrivateChannel().flatMap(c -> c.createMessage(spec -> spec.setEmbed(embed))).block();
    }

    public static Message sendDirectMessageSync(String message, Consumer<EmbedCreateSpec> embed, User user) {
        return user.getPrivateChannel().flatMap(c -> c.createMessage(spec -> spec.setContent(message).setEmbed(embed))).block();
    }

    //Message editing
    public static void editMessage(String message, Message original) {
        original.edit(spec -> spec.setContent(message)).subscribe();
    }

    public static void editMessage(String message, Consumer<EmbedCreateSpec> embed, Message original) {
        original.edit(spec -> spec.setContent(message).setEmbed(embed)).subscribe();
    }

    public static void editMessage(String message, MessageCreateEvent event) {
        event.getMessage().edit(spec -> spec.setContent(message)).subscribe();
    }

    public static void editMessage(String message, Consumer<EmbedCreateSpec> embed, MessageCreateEvent event) {
        event.getMessage().edit(spec -> spec.setContent(message).setEmbed(embed)).subscribe();
    }

    //Message deleting
    public static void deleteMessage(Message message) {
        message.delete().subscribe();
    }

    public static void deleteMessage(MessageCreateEvent event) {
        event.getMessage().delete().subscribe();
    }
}
