package org.dreamexposure.ticketbird.message;

import org.dreamexposure.ticketbird.Main;
import org.dreamexposure.ticketbird.file.ReadFile;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessageManager {
    public static String lineBreak = System.lineSeparator();

    private static Map<String, Map<String, String>> langs;

    public static void loadLangs() {
        langs = ReadFile.readAllLangFiles();
    }

    public static boolean reloadLangs() {
        try {
            langs = ReadFile.readAllLangFiles();
            return true;
        } catch (Exception e) {
            Logger.getLogger().exception(null, "Failed to reload lang files!", e, MessageManager.class);
            return false;
        }
    }

    public static List<String> getLangs() {

        return new ArrayList<>(langs.keySet());
    }

    public static boolean isSupported(String _value) {
        for (String l : langs.keySet()) {
            if (l.equalsIgnoreCase(_value)) {
                return true;
            }
        }
        return false;
    }

    public static String getValidLang(String _value) {
        for (String l : langs.keySet()) {
            if (l.equalsIgnoreCase(_value)) {
                return l;
            }
        }
        return "ENGLISH";
    }

    public static String getMessage(String key, GuildSettings settings) {
        Map<String, String> messages;

        if (settings.getLang() != null && langs.containsKey(settings.getLang())) {
            messages = langs.get(settings.getLang());
        } else {
            messages = langs.get("ENGLISH");
        }

        return messages.getOrDefault(key, "***FAILSAFE MESSAGE*** MESSAGE NOT FOUND!! Message requested: " + key).replace("%lb%", MessageManager.lineBreak);
    }

    public static String getMessage(String key, String var, String replace, GuildSettings settings) {
        Map<String, String> messages;

        if (settings.getLang() != null && langs.containsKey(settings.getLang())) {
            messages = langs.get(settings.getLang());
        } else {
            messages = langs.get("ENGLISH");
        }
        return messages.getOrDefault(key, "***FAILSAFE MESSAGE*** MESSAGE NOT FOUND!! Message requested: " + key).replace(var, replace).replace("%lb%", MessageManager.lineBreak);
    }

    public static IMessage sendMessage(String message, MessageReceivedEvent event) {
        return RequestBuffer.request(() -> {
            try {
                return new MessageBuilder(Main.getClient()).appendContent(message).withChannel(event.getMessage().getChannel()).build();
            } catch (DiscordException | MissingPermissionsException e) {
                //Failed to send message.
                return null;
            }
        }).get();
    }

    public static IMessage sendMessage(String message, IChannel channel) {
        return RequestBuffer.request(() -> {
            try {
                return new MessageBuilder(Main.getClient()).appendContent(message).withChannel(channel).build();
            } catch (DiscordException | MissingPermissionsException e) {
                //Failed to send message.
                return null;
            }
        }).get();
    }

    public static IMessage sendMessage(EmbedObject embed, MessageReceivedEvent event) {
        return RequestBuffer.request(() -> {
            try {
                return new MessageBuilder(Main.getClient()).withEmbed(embed).withChannel(event.getMessage().getChannel()).build();

            } catch (DiscordException | MissingPermissionsException e) {
                //Failed to send message.
                return null;
            }
        }).get();
    }

    public static IMessage sendMessage(EmbedObject embed, IChannel channel) {
        return RequestBuffer.request(() -> {
            try {
                return new MessageBuilder(Main.getClient()).withEmbed(embed).withChannel(channel).build();
            } catch (DiscordException | MissingPermissionsException e) {
                //Failed to send message.
                return null;
            }
        }).get();
    }

    public static IMessage sendMessage(EmbedObject embed, String message, MessageReceivedEvent event) {
        return RequestBuffer.request(() -> {
            try {
                return new MessageBuilder(Main.getClient()).appendContent(message).withEmbed(embed).withChannel(event.getMessage().getChannel()).build();
            } catch (DiscordException | MissingPermissionsException e) {
                //Failed to send message.
                return null;
            }
        }).get();
    }

    public static IMessage sendMessage(EmbedObject embed, String message, IChannel channel) {
        return RequestBuffer.request(() -> {
            try {
                return new MessageBuilder(Main.getClient()).appendContent(message).withEmbed(embed).withChannel(channel).build();
            } catch (DiscordException | MissingPermissionsException e) {
                //Failed to send message.
                return null;
            }
        }).get();
    }

    public static void sendMessageAsync(EmbedObject embedObject, String message, IChannel channel) {
        RequestBuffer.request(() -> {
            try {
                new MessageBuilder(Main.getClient()).appendContent(message).withEmbed(embedObject).withChannel(channel).build();
            } catch (DiscordException | MissingPermissionsException e) {
                //No reason to print exception.
            }
        });
    }

    public static IMessage sendDirectMessage(String message, IUser user) {
        return RequestBuffer.request(() -> {
            try {
                IPrivateChannel pc = user.getOrCreatePMChannel();
                return new MessageBuilder(Main.getClient()).withChannel(pc).appendContent(message).build();
            } catch (DiscordException | MissingPermissionsException e) {
                //Failed to send message.
                return null;
            }
        }).get();
    }

    public static IMessage sendDirectMessage(EmbedObject embed, IUser user) {
        return RequestBuffer.request(() -> {
            try {
                IPrivateChannel pc = user.getOrCreatePMChannel();
                return new MessageBuilder(Main.getClient()).withChannel(pc).withEmbed(embed).build();
            } catch (DiscordException | MissingPermissionsException e) {
                //Failed to send message.
                return null;
            }
        }).get();
    }

    public static IMessage sendDirectMessage(String message, EmbedObject embed, IUser user) {
        return RequestBuffer.request(() -> {
            try {
                IPrivateChannel pc = user.getOrCreatePMChannel();
                return new MessageBuilder(Main.getClient()).withChannel(pc).appendContent(message).withEmbed(embed).build();
            } catch (DiscordException | MissingPermissionsException e) {
                //Failed to send message.
                return null;
            }
        }).get();
    }

    public static boolean deleteMessage(MessageReceivedEvent event) {
        try {
            return RequestBuffer.request(() -> {
                try {
                    if (!event.getMessage().isDeleted()) {
                        event.getMessage().delete();
                    }
                    return true;
                } catch (DiscordException | MissingPermissionsException e) {
                    //Failed to delete
                    return false;
                }
            }).get();
        } catch (NullPointerException e) {
            return false;
        }
    }

    public static boolean deleteMessage(IMessage message) {
        try {
            return RequestBuffer.request(() -> {
                try {
                    if (!message.isDeleted()) {
                        message.delete();
                    }
                    return true;
                } catch (DiscordException | MissingPermissionsException e) {
                    //Failed to delete.
                    return false;
                }
            }).get();
        } catch (NullPointerException e) {
            return false;
        }
    }

    public static boolean editMessage(IMessage message, String content) {
        try {
            return RequestBuffer.request(() -> {
                try {
                    if (message != null && !message.isDeleted()) {
                        message.edit(content);
                    }
                    return true;
                } catch (DiscordException | MissingPermissionsException e) {
                    //Failed to edit.
                    return false;
                }
            }).get();
        } catch (NullPointerException e) {
            return false;
        }
    }

    public static boolean editMessage(IMessage message, String content, EmbedObject embed) {
        try {
            return RequestBuffer.request(() -> {
                try {
                    if (!message.isDeleted()) {
                        message.edit(content, embed);
                    }
                    return true;
                } catch (DiscordException | MissingPermissionsException e) {
                    //Failed to edit.
                    return false;
                }
            }).get();
        } catch (NullPointerException e) {
            return false;
        }
    }
}