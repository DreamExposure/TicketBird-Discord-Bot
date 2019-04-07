package org.dreamexposure.ticketbird.objects.guild;

import discord4j.core.object.util.Snowflake;

public class Project {
    private final Snowflake guildId;
    private final String name;
    private String prefix;

    public Project(Snowflake _guildId, String _name) {
        name = _name;
        guildId = _guildId;
    }

    //Getters
    public Snowflake getGuildId() {
        return guildId;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    //Setters
    public void setPrefix(String _prefix) {
        prefix = _prefix;
    }
}