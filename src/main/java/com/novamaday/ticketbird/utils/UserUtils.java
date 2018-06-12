package com.novamaday.ticketbird.utils;

import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserUtils {
    public static long getUser(String toLookFor, IGuild guild) {
        toLookFor = GeneralUtils.trim(toLookFor);
        final String lower = toLookFor.toLowerCase();
        if (lower.matches("@!?[0-9]+") || lower.matches("[0-9]+")) {
            final String parse = toLookFor.replaceAll("[<@!>]", "");
            IUser exists = guild.getUserByID(Long.parseLong(toLookFor.replaceAll("[<@!>]", "")));
            if (exists != null) {
                return exists.getLongID();
            }
        }


        List<IUser> users = new ArrayList<>();
        List<IUser> us = guild.getUsers();
        users.addAll(us.stream().filter(u -> u.getName().equalsIgnoreCase(lower)).collect(Collectors.toList()));
        users.addAll(us.stream().filter(u -> u.getName().toLowerCase().contains(lower)).collect(Collectors.toList()));
        users.addAll(us.stream().filter(u -> (u.getName() + "#" + u.getDiscriminator()).equalsIgnoreCase(lower)).collect(Collectors.toList()));
        users.addAll(us.stream().filter(u -> u.getDiscriminator().equalsIgnoreCase(lower)).collect(Collectors.toList()));
        users.addAll(us.stream().filter(u -> u.getDisplayName(guild).equalsIgnoreCase(lower)).collect(Collectors.toList()));
        users.addAll(us.stream().filter(u -> u.getDisplayName(guild).toLowerCase().contains(lower)).collect(Collectors.toList()));


        if (!users.isEmpty()) {
            return users.get(0).getLongID();
        }

        return 0;
    }
}