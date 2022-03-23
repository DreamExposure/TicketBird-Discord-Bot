package org.dreamexposure.ticketbird.utils;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.http.client.ClientException;
import org.dreamexposure.ticketbird.database.DatabaseManager;
import org.dreamexposure.ticketbird.object.GuildSettings;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;
import reactor.math.MathFlux;

import java.time.Instant;
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

    public static Mono<EmbedCreateSpec> getStaticSupportMessageEmbed(Guild guild, GuildSettings settings) {
        if (settings.getAwaitingCategory() == null || settings.getRespondedCategory() == null
            || settings.getHoldCategory() == null) return Mono.empty();

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

            return EmbedCreateSpec.builder()
                .author("TicketBird", null, GlobalVars.INSTANCE.getIconUrl())
                .color(GlobalVars.INSTANCE.getEmbedColor())
                .title("Help Desk")
                .description("Need help with something? Open a new ticket by clicking the button below.")
                .addField("Tickets Currently Open", open + "", true)
                .addField("Tickets on Hold", hold + "", true)
                .addField("Total Tickets Closed", closed + "", true)
                .footer("Last Update", null)
                .timestamp(Instant.now())
                .build();
        }));
    }

    public static Mono<Message> updateStaticMessage(Guild guild, GuildSettings settings) {
        var button = Button.primary("ticketbird-create-ticket", ReactionEmoji.codepoints("TODO"), "Open Ticket");

        if (settings.getSupportChannel() == null || settings.getStaticMessage() == null) return Mono.empty();

        return guild.getChannelById(settings.getSupportChannel()).ofType(TextChannel.class).flatMap(channel ->
            channel.getMessageById(settings.getStaticMessage()).flatMap(message -> {
                // Message exists, just needs the edit
                return getStaticSupportMessageEmbed(guild, settings).flatMap(spec ->
                    message.edit()
                        .withEmbeds(spec)
                        .withComponents(ActionRow.of(button))
                );
            }).onErrorResume(ClientException.isStatusCode(404), e -> {
                // Message deleted, recreate it
                return getStaticSupportMessageEmbed(guild, settings).flatMap(spec ->
                    channel.createMessage(spec)
                        .withComponents(ActionRow.of(button))
                ).flatMap(msg -> {
                    settings.setStaticMessage(msg.getId());

                    return Mono.fromCallable(() -> DatabaseManager.getManager().updateSettings(settings))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(msg);
                });
            })).onErrorResume(ClientException.isStatusCode(403, 404), e -> {
            //Permission denied or channel deleted.
            settings.setStaticMessage(null);

            return Mono.fromCallable(() -> DatabaseManager.getManager().updateSettings(settings))
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.empty());
        });
    }
}
