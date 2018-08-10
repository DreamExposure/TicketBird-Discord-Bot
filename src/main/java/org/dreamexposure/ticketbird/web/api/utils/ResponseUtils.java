package org.dreamexposure.ticketbird.web.api.utils;

public class ResponseUtils {
    public static String asMessage(String message) {
        return "{\"Message\": \"" + message + "\"";
    }
}