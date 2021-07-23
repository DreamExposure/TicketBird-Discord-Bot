package org.dreamexposure.ticketbird.message;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.TextChannel;
import org.springframework.lang.Nullable;

public class ChannelManager {
    public static Category createCategory(String name, Guild guild) {
        return guild.createCategory(name)
            .withReason("TicketBird Setup")
            .block();
    }

    public static TextChannel createChannel(String name, String topic, @Nullable Snowflake awaitingCat, Guild guild) {
        var spec =  guild.createTextChannel(name)
            .withTopic(topic)
            .withReason("New Ticket or TicketBird Setup");

        if (awaitingCat != null)
            spec = spec.withParentId(awaitingCat);

        return spec.block();
    }

    public static void deleteCategoryOrChannelAsync(Snowflake id, Guild guild) {
        guild.getChannelById(id)
            .flatMap(Channel::delete)
            .subscribe();
    }
}
