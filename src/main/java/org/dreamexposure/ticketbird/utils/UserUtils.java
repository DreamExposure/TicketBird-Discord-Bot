package org.dreamexposure.ticketbird.utils;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

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


        List<Member> users = new ArrayList<>();

        users.addAll(guild.getMembers().filter(m -> m.getUsername().equalsIgnoreCase(lower)).collectList().block());
        users.addAll(guild.getMembers().filter(m -> m.getUsername().toLowerCase().contains(lower)).collectList().block());
        users.addAll(guild.getMembers().filter(m -> (m.getUsername() + "#" + m.getDiscriminator()).equalsIgnoreCase(lower)).collectList().block());
        users.addAll(guild.getMembers().filter(m -> m.getDiscriminator().equalsIgnoreCase(lower)).collectList().block());
        users.addAll(guild.getMembers().filter(m -> m.getDisplayName().equalsIgnoreCase(lower)).collectList().block());
        users.addAll(guild.getMembers().filter(m -> m.getDisplayName().toLowerCase().contains(lower)).collectList().block());


        if (!users.isEmpty())
            return users.get(0).getId();

        return null;
    }

}