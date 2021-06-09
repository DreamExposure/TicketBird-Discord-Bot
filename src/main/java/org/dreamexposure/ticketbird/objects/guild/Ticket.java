package org.dreamexposure.ticketbird.objects.guild;

import discord4j.common.util.Snowflake;

public class Ticket {
    private final Snowflake guildId;
    private final int number;
    private String project;
    private Snowflake creator;
    private Snowflake channel;
    private Snowflake category;

    private long lastActivity;

    public Ticket(Snowflake _guildId, int _number) {
        guildId = _guildId;
        number = _number;
        project = "N/a";
        lastActivity = System.currentTimeMillis();

        creator = null;
    }

    //Getters
    public Snowflake getGuildId() {
        return guildId;
    }

    public int getNumber() {
        return number;
    }

    public String getProject() {
        return project;
    }

    public Snowflake getCreator() {
        return creator;
    }

    public Snowflake getChannel() {
        return channel;
    }

    public Snowflake getCategory() {
        return category;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    //Setters
    public void setProject(String _project) {
        project = _project;
    }

    public void setCreator(Snowflake _creator) {
        creator = _creator;
    }

    public void setChannel(Snowflake _channel) {
        channel = _channel;
    }

    public void setCategory(Snowflake _category) {
        category = _category;
    }

    public void setLastActivity(long _activity) {
        lastActivity = _activity;
    }
}
