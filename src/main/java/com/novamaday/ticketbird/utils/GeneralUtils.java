package com.novamaday.ticketbird.utils;

import com.novamaday.ticketbird.database.DatabaseManager;
import com.novamaday.ticketbird.message.MessageManager;
import com.novamaday.ticketbird.objects.guild.GuildSettings;
import sx.blah.discord.handle.obj.ICategory;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.util.Random;

public class GeneralUtils {
    /**
     * Gets the contents of the message at a set offset.
     *
     * @param args   The args of the command.
     * @param offset The offset in the string.
     * @return The contents of the message at a set offset.
     */
    public static String getContent(String[] args, int offset) {
        StringBuilder content = new StringBuilder();
        for (int i = offset; i < args.length; i++) {
            content.append(args[i]).append(" ");
        }
        return content.toString().trim();
    }

    /**
     * Trims the string front to back.
     *
     * @param str The String to trim.
     * @return The trimmed string.
     */
    public static String trim(String str) {
        while (str.length() > 1 && str.charAt(0) == ' ') {
            str = str.substring(1);
        }
        return str.trim();
    }

    /**
     * Combines the arguments of a String array
     *
     * @param args  The string array
     * @param start What index to start at
     * @return The combines arguments
     */
    public static String combineArgs(String[] args, int start) {
        if (start >= args.length)
            throw new IllegalArgumentException("You can not start at an index that doesn't exit!");

        StringBuilder res = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            res.append(args[i]).append(" ");
        }
        return res.toString().trim();
    }

    /**
     * This is an overkill parser made by xaanit. You can thank him for this nightmare.
     * <br> <br>
     * regardless, it works, and therefore we will use it because generally speaking it seems some users do not understand that "<" and ">" are not in fact required and are just symbols <b>CLEARLY DEFINED</b> in our documentation.
     *
     * @param str The string to parse.
     * @return The string, but without the user errors.
     */
    public static String overkillParser(String str) {
        Random random = new Random(str.length() * 2 >>> 4 & 3);
        StringBuilder leftFace = new StringBuilder();
        StringBuilder rightFace = new StringBuilder();
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        for (int i = 0; i < 30; i++) {
            leftFace.append(alphabet.charAt(random.nextInt(alphabet.length())));
            rightFace.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return str.replace("<<", leftFace.toString()).replace(">>", rightFace.toString()).replace("<", "").replace(">", "").replace(leftFace.toString(), "<").replace(rightFace.toString(), ">");
    }

    public static String getNormalStaticSupportMessage(IGuild guild, GuildSettings settings) {
        String msg = MessageManager.getMessage("Support.StaticMessage.Normal", settings);

        ICategory awaiting = guild.getCategoryByID(settings.getAwaitingCategory());
        ICategory responded = guild.getCategoryByID(settings.getRespondedCategory());
        ICategory hold = guild.getCategoryByID(settings.getHoldCategory());

        int allTickets = settings.getNextId() - 1;
        int closedCount = allTickets - awaiting.getChannels().size() - responded.getChannels().size() - hold.getChannels().size();

        msg = msg.replace("%open%", String.valueOf(awaiting.getChannels().size() + responded.getChannels().size()));
        msg = msg.replace("%hold%", hold.getChannels().size() + "");
        msg = msg.replace("%closed%", closedCount + "");

        return msg;
    }

    public static String getHighVoluneStaticSupportMessage(IGuild guild, GuildSettings settings) {
        String msg = MessageManager.getMessage("Support.StaticMessage.HighVolume", settings);

        ICategory awaiting = guild.getCategoryByID(settings.getAwaitingCategory());
        ICategory responded = guild.getCategoryByID(settings.getRespondedCategory());
        ICategory hold = guild.getCategoryByID(settings.getHoldCategory());

        int allTickets = settings.getNextId() - 1;
        int closedCount = allTickets - awaiting.getChannels().size() - responded.getChannels().size() - hold.getChannels().size();

        msg = msg.replace("%open%", String.valueOf(awaiting.getChannels().size() + responded.getChannels().size()));
        msg = msg.replace("%hold%", hold.getChannels().size() + "");
        msg = msg.replace("%closed%", closedCount + "");

        return msg;
    }

    public static void updateStaticMessage(IGuild guild, GuildSettings settings) {
        IChannel supportChannel = guild.getChannelByID(settings.getSupportChannel());

        IMessage staticMsg = supportChannel.fetchMessage(settings.getStaticMessage());
        if (staticMsg != null) {
            //Edit static message...
            MessageManager.editMessage(staticMsg, GeneralUtils.getNormalStaticSupportMessage(guild, settings));
        } else {
            //Somehow the static message was deleted, let's just recreate it.

            settings.setStaticMessage(MessageManager.sendMessage(GeneralUtils.getNormalStaticSupportMessage(guild, settings), supportChannel).getLongID());

            DatabaseManager.getManager().updateSettings(settings);
        }
    }
}