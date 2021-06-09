package org.dreamexposure.ticketbird.utils;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;
import org.dreamexposure.ticketbird.database.DatabaseManager;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.message.MessageManager;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import reactor.core.publisher.Mono;

import java.util.Random;

@SuppressWarnings({"ConstantConditions", "Duplicates"})
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

    public static long getOpenTicketCount(Guild guild, GuildSettings settings) {
        Category awaiting = guild.getChannelById(settings.getAwaitingCategory()).ofType(Category.class).block();
        Category responded = guild.getChannelById(settings.getRespondedCategory()).ofType(Category.class).block();

        return awaiting.getChannels().count().block() + responded.getChannels().count().block();
    }

    public static String getNormalStaticSupportMessage(Guild guild, GuildSettings settings) {
        String msg = MessageManager.getMessage("Support.StaticMessage.Normal", settings);

        Category awaiting = guild.getChannelById(settings.getAwaitingCategory()).ofType(Category.class).block();
        Category responded = guild.getChannelById(settings.getRespondedCategory()).ofType(Category.class).block();
        Category hold = guild.getChannelById(settings.getHoldCategory()).ofType(Category.class).block();

        int allTickets = settings.getNextId() - 1;
        long closedCount = allTickets - awaiting.getChannels().count().block() - responded.getChannels().count().block() - hold.getChannels().count().block();

        msg = msg.replace("%open%", String.valueOf(awaiting.getChannels().count().block() + responded.getChannels().count().block()));
        msg = msg.replace("%hold%", hold.getChannels().count().block() + "");
        msg = msg.replace("%closed%", closedCount + "");

        return msg;
    }

    public static String getHighVolumeStaticSupportMessage(Guild guild, GuildSettings settings) {
        String msg = MessageManager.getMessage("Support.StaticMessage.HighVolume", settings);

        Category awaiting = guild.getChannelById(settings.getAwaitingCategory()).ofType(Category.class).block();
        Category responded = guild.getChannelById(settings.getRespondedCategory()).ofType(Category.class).block();
        Category hold = guild.getChannelById(settings.getHoldCategory()).ofType(Category.class).block();

        int allTickets = settings.getNextId() - 1;

        long closedCount = allTickets - awaiting.getChannels().count().block() - responded.getChannels().count().block() - hold.getChannels().count().block();

        msg = msg.replace("%open%", String.valueOf(awaiting.getChannels().count().block() + responded.getChannels().count().block()));
        msg = msg.replace("%hold%", hold.getChannels().count().block() + "");
        msg = msg.replace("%closed%", closedCount + "");

        return msg;
    }

    public static void updateStaticMessage(Guild guild, GuildSettings settings) {
        try {
            TextChannel supportChannel = guild.getChannelById(settings.getSupportChannel()).ofType(TextChannel.class).block();

            if (supportChannel == null) {
                //Something must have gone wrong, we lost the support channel... just ignore the update request
                return;
            }
            Message staticMsg = supportChannel.getMessageById(settings.getStaticMessage()).onErrorResume(e -> Mono.empty()).block();

            String messageContent;
            if (getOpenTicketCount(guild, settings) > 25)
                messageContent = getHighVolumeStaticSupportMessage(guild, settings);
            else
                messageContent = getNormalStaticSupportMessage(guild, settings);

            if (staticMsg != null) {
                //Edit static message...
                MessageManager.editMessage(messageContent, staticMsg);
            } else {
                //Somehow the static message was deleted, let's just recreate it.
                settings.setStaticMessage(MessageManager.sendMessageSync(messageContent, supportChannel).getId());

                DatabaseManager.getManager().updateSettings(settings);
            }
        } catch (Exception e) {
            Logger.getLogger().exception(null, "Failed to handle Static Message update!", e, true, GeneralUtils.class);
        }
    }
}
