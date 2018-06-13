package com.novamaday.ticketbird.objects.guild;

public class Ticket {
    private final long guildId;
    private final int number;
    private String project;
    private long creator;
    private long channel;
    private long category;

    private long lastActivity;

    public Ticket(long _guildId, int _number) {
        guildId = _guildId;
        number = _number;
        project = "N/a";
        lastActivity = System.currentTimeMillis();
    }

    //Getters
    public long getGuildId() {
        return guildId;
    }

    public int getNumber() {
        return number;
    }

    public String getProject() {
        return project;
    }

    public long getCreator() {
        return creator;
    }

    public long getChannel() {
        return channel;
    }

    public long getCategory() {
        return category;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    //Setters
    public void setProject(String _project) {
        project = _project;
    }

    public void setCreator(long _creator) {
        creator = _creator;
    }

    public void setChannel(long _channel) {
        channel = _channel;
    }

    public void setCategory(long _category) {
        category = _category;
    }

    public void setLastActivity(long _activity) {
        lastActivity = _activity;
    }
}