package org.dreamexposure.ticketbird.message;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import org.dreamexposure.ticketbird.file.ReadFile;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import org.dreamexposure.ticketbird.utils.GlobalVars;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"UnusedReturnValue", "unused"})
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

    public static List<String> getLangs() {

        return new ArrayList<>(langs.keySet());
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
        channel.createMessage(message).subscribe();
    }

    public static void sendMessageAsync(EmbedCreateSpec embed, TextChannel channel) {
        channel.createEmbed(embed).subscribe();
    }

    public static void sendMessageAsync(String message, EmbedCreateSpec embed, TextChannel channel) {
        channel.createMessage(message)
            .withEmbed(embed)
            .subscribe();
    }

    public static void sendMessageAsync(String message, MessageCreateEvent event) {
        event.getMessage().getChannel()
            .flatMap(c -> c.createMessage(message))
            .subscribe();
    }

    public static void sendMessageAsync(EmbedCreateSpec embed, MessageCreateEvent event) {
        event.getMessage().getChannel()
            .flatMap(c -> c.createEmbed(embed))
            .subscribe();
    }

    public static void sendMessageAsync(String message, EmbedCreateSpec embed, MessageCreateEvent event) {
        event.getMessage().getChannel()
            .flatMap( c -> c.createMessage(message).withEmbed(embed))
            .subscribe();
    }

    public static Message sendMessageSync(String message, MessageCreateEvent event) {
        return event.getMessage().getChannel()
            .flatMap(c -> c.createMessage(message))
            .block();
    }

    public static Message sendMessageSync(EmbedCreateSpec embed, MessageCreateEvent event) {
        return event.getMessage().getChannel()
            .flatMap(c -> c.createEmbed(embed))
            .block();
    }

    public static Message sendMessageSync(String message, EmbedCreateSpec embed, MessageCreateEvent event) {
        return event.getMessage().getChannel()
            .flatMap(c -> c.createMessage(message).withEmbed(embed))
            .block();
    }

    public static Message sendMessageSync(String message, TextChannel channel) {
        return channel.createMessage(message).block();
    }

    public static Message sendMessageSync(EmbedCreateSpec embed, TextChannel channel) {
        return channel.createEmbed(embed).block();
    }

    public static Message sendMessageSync(String message, EmbedCreateSpec embed, TextChannel channel) {
        return channel.createMessage(message)
            .withEmbed(embed)
            .block();
    }

    public static void sendDirectMessageAsync(String message, User user) {
        user.getPrivateChannel()
            .flatMap(c -> c.createMessage(message))
            .subscribe();
    }

    public static void sendDirectMessageAsync(EmbedCreateSpec embed, User user) {
        user.getPrivateChannel()
            .flatMap(c -> c.createEmbed(embed))
            .subscribe();
    }

    public static void sendDirectMessageAsync(String message, EmbedCreateSpec embed, User user) {
        user.getPrivateChannel()
            .flatMap(c -> c.createMessage(message).withEmbed(embed))
            .subscribe();
    }

    public static Message sendDirectMessageSync(String message, User user) {
        return user.getPrivateChannel()
            .flatMap(c -> c.createMessage(message))
            .block();
    }

    public static Message sendDirectMessageSync(EmbedCreateSpec embed, User user) {
        return user.getPrivateChannel()
            .flatMap(c -> c.createEmbed(embed))
            .block();
    }

    public static Message sendDirectMessageSync(String message, EmbedCreateSpec embed, User user) {
        return user.getPrivateChannel()
            .flatMap(c -> c.createMessage(message).withEmbed(embed))
            .block();
    }

    //Message editing
    public static void editMessage(String message, Message original) {
        original.edit()
            .withContentOrNull(message)
            .subscribe();
    }

    public static void editMessage(String message, EmbedCreateSpec embed, Message original) {
        original.edit()
            .withContentOrNull(message)
            .withEmbedOrNull(embed)
            .subscribe();
    }

    public static void editMessage(String message, MessageCreateEvent event) {
        event.getMessage().edit()
            .withContentOrNull(message)
            .subscribe();
    }

    public static void editMessage(String message, EmbedCreateSpec embed, MessageCreateEvent event) {
        event.getMessage().edit()
            .withContentOrNull(message)
            .withEmbedOrNull(embed)
            .subscribe();
    }

    //Message deleting
    public static void deleteMessage(Message message) {
        message.delete().subscribe();
    }

    public static void deleteMessage(MessageCreateEvent event) {
        event.getMessage().delete().subscribe();
    }
}
