package org.dreamexposure.ticketbird.message;

import discord4j.core.object.entity.Category;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.CategoryCreateSpec;
import discord4j.core.spec.TextChannelCreateSpec;
import discord4j.core.spec.TextChannelEditSpec;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class ChannelManager {
    public static Category createCategory(String name, Guild guild) {
        Consumer<CategoryCreateSpec> cat = spec -> {
            spec.setName(name);
            spec.setReason("TicketBird Setup");
        };
        return guild.createCategory(cat).block();
    }

    public static void createCategoryAsync(String name, Guild guild) {
        Consumer<CategoryCreateSpec> cat = spec -> {
            spec.setName(name);
            spec.setReason("TicketBird Setup");
        };
        guild.createCategory(cat).subscribe();
    }

    public static TextChannel createChannel(String name, String topic, @Nullable Snowflake awaitingCat, Guild guild) {
        Consumer<TextChannelCreateSpec> chan = spec -> {
          spec.setName(name);
          spec.setTopic(topic);
          if (awaitingCat != null)
              spec.setParentId(awaitingCat);
          spec.setReason("New Ticket or TicketBird Setup");
        };
        return guild.createTextChannel(chan).block();
    }

    public static void createChannelAsync(String name, Guild guild) {
        Consumer<TextChannelCreateSpec> chan = spec -> {
            spec.setName(name);
            spec.setReason("New Ticket or TicketBird Setup");
        };
        guild.createTextChannel(chan).subscribe();
    }

    public static void deleteCategoryOrChannelAsync(Snowflake id, Guild guild) {
        guild.getChannelById(id).flatMap(Channel::delete).subscribe();
    }


    public static void moveChannelAsync(Snowflake id, Snowflake catId, Guild guild) {
        Consumer<TextChannelEditSpec> edit = spec -> spec.setParentId(catId);

        guild.getChannelById(id).ofType(TextChannel.class).flatMap(c -> c.edit(edit)).subscribe();

    }
}