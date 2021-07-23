package org.dreamexposure.ticketbird.utils;

import discord4j.common.util.Snowflake;
import discord4j.rest.util.Color;

public class GlobalVars {
    public static String iconUrl;

    public final static String lineBreak = System.lineSeparator();

    public final static Snowflake novaId = Snowflake.of(130510525770629121L);

    public final static Color embedColor = Color.of(252, 113, 20);

    public final static String[] disallowed = new String[]{"!", "@", "#", "$", "%", "^", "&", "*",
        "(", ")", "=", "+", "[", "]", "{", "}", "|", "\\", ";", ":", "'", "\"", ",", ".", "<", ">", "?", "/", "`", "~"};
}
