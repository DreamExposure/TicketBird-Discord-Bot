package org.dreamexposure.ticketbird.objects.guild;

import discord4j.common.util.Snowflake;

import java.util.ArrayList;
import java.util.List;

public class GuildSettings {
    private final Snowflake guildID;

    private String lang;
    private String prefix;

    private boolean patronGuild;
    private boolean devGuild;

    private boolean useProjects;

    private Snowflake awaitingCategory;
    private Snowflake respondedCategory;
    private Snowflake holdCategory;
    private Snowflake closeCategory;

    private Snowflake supportChannel;
    private Snowflake staticMessage;

    private int nextId;

    private int totalClosed;

    private final List<Snowflake> staff = new ArrayList<>();

    public GuildSettings(Snowflake _guildId) {
        guildID = _guildId;

        lang = "ENGLISH";
        prefix = "=";

        patronGuild = false;
        devGuild = false;

        nextId = 1;
        totalClosed = 0;
    }

    //Getters
    public Snowflake getGuildID() {
        return guildID;
    }

    public String getLang() {
        return lang;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isPatronGuild() {
        return patronGuild;
    }

    public boolean isDevGuild() {
        return devGuild;
    }

    public boolean isUseProjects() {
        return useProjects;
    }

    public Snowflake getAwaitingCategory() {
        return awaitingCategory;
    }

    public Snowflake getRespondedCategory() {
        return respondedCategory;
    }

    public Snowflake getHoldCategory() {
        return holdCategory;
    }

    public Snowflake getCloseCategory() {
        return closeCategory;
    }

    public Snowflake getSupportChannel() {
        return supportChannel;
    }

    public Snowflake getStaticMessage() {
        return staticMessage;
    }

    public int getNextId() {
        return nextId;
    }

    public int getTotalClosed() {
        return totalClosed;
    }

    public List<Snowflake> getStaff() {
        return staff;
    }

    public String getStaffString() {
        StringBuilder list = new StringBuilder();
        for (Snowflake s : staff) {
            list.append(s.asString()).append(",");
        }

        return list.toString();
    }

    //Setters
    public void setLang(String _lang) {
        lang = _lang;
    }

    public void setPrefix(String _prefix) {
        prefix = _prefix;
    }

    public void setPatronGuild(boolean _patronGuild) {
        patronGuild = _patronGuild;
    }

    public void setDevGuild(boolean _devGuild) {
        devGuild = _devGuild;
    }

    public void setUseProjects(boolean useProjects) {
        this.useProjects = useProjects;
    }

    public void setAwaitingCategory(Snowflake _awaiting) {
        awaitingCategory = _awaiting;
    }

    public void setRespondedCategory(Snowflake _responded) {
        respondedCategory = _responded;
    }

    public void setHoldCategory(Snowflake _hold) {
        holdCategory = _hold;
    }

    public void setCloseCategory(Snowflake _close) {
        closeCategory = _close;
    }

    public void setSupportChannel(Snowflake _support) {
        supportChannel = _support;
    }

    public void setStaticMessage(Snowflake _static) {
        staticMessage = _static;
    }

    public void setNextId(int _next) {
        nextId = _next;
    }

    public void setTotalClosed(int _total) {
        totalClosed = _total;
    }

    public void setStaffFromString(String _staff) {
        for (String s : _staff.split(",")) {
            try {
                staff.add(Snowflake.of(s));
            } catch (NumberFormatException ignore) {
            }
        }
    }
}
