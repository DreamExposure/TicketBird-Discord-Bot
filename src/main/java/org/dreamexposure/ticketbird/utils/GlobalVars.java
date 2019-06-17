package org.dreamexposure.ticketbird.utils;

import discord4j.core.object.util.Snowflake;

import java.awt.*;

public class GlobalVars {
    public static String iconUrl;
    public final static String siteUrl = "https://ticketbird.dreamexposure.org";

    public final static String lineBreak = System.lineSeparator();

    public final static Snowflake novaId = Snowflake.of(130510525770629121L);

    public final static String version = "1.0.0";

    public final static Color embedColor = new Color(252, 113, 20);

    public final static long oneWeekMs = 604800000;

    public final static long oneDayMs = 86400000;

    public final static long oneHourMs = 3600000;

    public final static String[] disallowed = new String[]{"!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "=", "+", "[", "]", "{", "}", "|", "\\", ";", ":", "'", "\"", ",", ".", "<", ">", "?", "/", "`", "~"};
}