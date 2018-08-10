package org.dreamexposure.ticketbird.objects.guild;

public class Project {
    private final long guildId;
    private final String name;
    private String prefix;

    public Project(long _guildId, String _name) {
        name = _name;
        guildId = _guildId;
    }

    //Getters
    public long getGuildId() {
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