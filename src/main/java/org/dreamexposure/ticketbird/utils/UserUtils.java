package org.dreamexposure.ticketbird.utils;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import reactor.core.publisher.Mono;

@SuppressWarnings("ConstantConditions")
public class UserUtils {
    public static Snowflake getUser(String toLookFor, Guild guild) {
        toLookFor = GeneralUtils.trim(toLookFor);
        final String lower = toLookFor.toLowerCase();
        if (lower.matches("@!?[0-9]+") || lower.matches("[0-9]+")) {
            Member exists = guild.getMemberById(Snowflake.of(Long.parseLong(toLookFor.replaceAll("[<@!>]", "")))).onErrorResume(e -> Mono.empty()).block();
            if (exists != null)
                return exists.getId();
        }

        return guild.getMembers().filter(m ->
            m.getUsername().equalsIgnoreCase(lower) ||
                m.getUsername().contains(lower) ||
                (m.getUsername() + "#" + m.getDiscriminator()).equalsIgnoreCase(lower) ||
                m.getDisplayName().equalsIgnoreCase(lower) ||
                m.getDisplayName().toLowerCase().contains(lower)
        ).blockFirst().getId();
    }

}
