package org.dreamexposure.ticketbird.utils;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.http.client.ClientException;
import org.dreamexposure.ticketbird.database.DatabaseManager;
import org.dreamexposure.ticketbird.message.MessageManager;
import org.dreamexposure.ticketbird.object.GuildSettings;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.math.MathFlux;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

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
        Random random = new Random(str.length() * 2L >>> 4 & 3);
        StringBuilder leftFace = new StringBuilder();
        StringBuilder rightFace = new StringBuilder();
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        for (int i = 0; i < 30; i++) {
            leftFace.append(alphabet.charAt(random.nextInt(alphabet.length())));
            rightFace.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return str.replace("<<", leftFace.toString()).replace(">>", rightFace.toString()).replace("<", "").replace(">", "").replace(leftFace.toString(), "<").replace(rightFace.toString(), ">");
    }

    public static Mono<Long> getOpenTicketCount(Guild guild, GuildSettings settings) {
        var awaiting = guild.getChannelById(settings.getAwaitingCategory())
            .ofType(Category.class)
            .flatMap(it -> it.getChannels().count());
        var responded = guild.getChannelById(settings.getRespondedCategory())
            .ofType(Category.class)
            .flatMap(it -> it.getChannels().count());

        return MathFlux.sumLong(awaiting.mergeWith(responded));

    }

    public static Mono<MessageCreateSpec> getNormalStaticSupportMessage(Guild guild, GuildSettings settings) {
        Mono<Long> awaitingMono = guild.getChannelById(settings.getAwaitingCategory()).ofType(Category.class)
            .flatMap(it -> it.getChannels().count());
        Mono<Long> respondedMono = guild.getChannelById(settings.getRespondedCategory()).ofType(Category.class)
            .flatMap(it -> it.getChannels().count());
        Mono<Long> holdMono = guild.getChannelById(settings.getHoldCategory()).ofType(Category.class)
            .flatMap(it -> it.getChannels().count());

        return Mono.zip(awaitingMono, respondedMono, holdMono).map(TupleUtils.function((await, respond, hold) -> {
            int allTickets = settings.getNextId() - 1;
            long closed = allTickets - await - respond - hold;
            long open = await + respond;

            var embed = EmbedCreateSpec.builder()
                .author("TicketBird", null,GlobalVars.INSTANCE.getIconUrl())
                .color(GlobalVars.INSTANCE.getEmbedColor())
                .title("Help Desk")
                .description("Need help with something? Open a new ticket by clicking the button below.")
                .addField("Tickets Currently Open", open + "", true)
                .addField("Tickets on Hold", hold + "", true)
                .addField("Total Tickets Closed", closed + "", true)
                .footer("Last Update", null)
                .timestamp(Instant.now())
                .build();
            var button = Button.primary("ticketbird-create-ticket", ReactionEmoji.codepoints("TODO"), "Open Ticket");

            return MessageCreateSpec.builder()
                .addEmbed(embed)
                .addComponent(ActionRow.of(button))
                .build();
        }));
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

    public static Mono<Message> updateStaticMessage(Guild guild, GuildSettings settings) {
        if (settings.getSupportChannel() == null || settings.getStaticMessage() == null) return Mono.empty();

        guild.getChannelById(settings.getSupportChannel()).ofType(TextChannel.class).flatMap(channel -> {
            return channel.getMessageById(settings.getStaticMessage()).flatMap(message -> {
                //TODO
            }).onErrorResume(ClientException.isStatusCode(404), e -> {
                // Message deleted, recreate it
                //TODO
            });
        }).onErrorResume(ClientException.isStatusCode(403, 404),e -> {
            //Permission denied or channel deleted.
            //TODO
        });
    }
}
