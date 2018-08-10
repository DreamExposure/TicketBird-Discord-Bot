package org.dreamexposure.ticketbird.message;

import sx.blah.discord.handle.obj.ICategory;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

public class ChannelManager {
    public static ICategory createCategory(String name, IGuild guild) {
        return RequestBuffer.request(() -> {
            try {
                return guild.createCategory(name);
            } catch (DiscordException | MissingPermissionsException ignore) {
                //Failed to create category.
                return null;
            }
        }).get();
    }

    public static void createCategoryAsync(String name, IGuild guild) {
        RequestBuffer.request(() -> {
            try {
                guild.createCategory(name);
            } catch (DiscordException | MissingPermissionsException ignore) {
            }
        });
    }

    public static IChannel createChannel(String name, IGuild guild) {
        return RequestBuffer.request(() -> {
            try {
                return guild.createChannel(name);
            } catch (DiscordException | MissingPermissionsException ignore) {
                //Failed to create channel.
                return null;
            }
        }).get();
    }

    public static void createChannelAsync(String name, IGuild guild) {
        RequestBuffer.request(() -> {
            try {
                guild.createChannel(name);
            } catch (DiscordException | MissingPermissionsException ignore) {
            }
        });
    }

    public static boolean deleteCategory(long id, IGuild guild) {
        return RequestBuffer.request(() -> {
            try {
                guild.getCategoryByID(id).delete();
                return true;
            } catch (DiscordException | MissingPermissionsException ignore) {
                return false;
            }
        }).get();
    }

    public static void deleteCategoryAsync(long id, IGuild guild) {
        RequestBuffer.request(() -> {
            try {
                guild.getCategoryByID(id).delete();
            } catch (DiscordException | MissingPermissionsException ignore) {

            }
        });
    }

    public static boolean deleteChannel(long id, IGuild guild) {
        return RequestBuffer.request(() -> {
            try {
                guild.getChannelByID(id).delete();
                return true;
            } catch (DiscordException | MissingPermissionsException ignore) {
                return false;
            }
        }).get();
    }

    public static void deleteChannelAsync(long id, IGuild guild) {
        RequestBuffer.request(() -> {
            try {
                guild.getChannelByID(id).delete();
            } catch (DiscordException | MissingPermissionsException ignore) {
            }
        });
    }

    public static boolean moveChannel(long id, long catId, IGuild guild) {
        return RequestBuffer.request(() -> {
            try {
                guild.getChannelByID(id).changeCategory(guild.getCategoryByID(catId));
                return true;
            } catch (DiscordException | MissingPermissionsException ignore) {
                return false;
            }
        }).get();
    }

    public static void moveChannelAsync(long id, long catId, IGuild guild) {
        RequestBuffer.request(() -> {
            try {
                guild.getChannelByID(id).changeCategory(guild.getCategoryByID(catId));
            } catch (DiscordException | MissingPermissionsException ignore) {
            }
        });
    }
}