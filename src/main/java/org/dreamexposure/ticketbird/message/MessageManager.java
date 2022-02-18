package org.dreamexposure.ticketbird.message;

import org.dreamexposure.ticketbird.file.ReadFile;
import org.dreamexposure.ticketbird.object.GuildSettings;
import org.dreamexposure.ticketbird.utils.GlobalVars;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class MessageManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(MessageManager.class);
    private static JSONObject langs;

    //Lang handling
    public static boolean reloadLangs() {
        try {
            langs = ReadFile.readAllLangFiles();
            return true;
        } catch (Exception e) {
           LOGGER.error(GlobalVars.INSTANCE.getDEFAULT(), "Failed to reload lang files!", e);
            return false;
        }
    }

    public static List<String> getLangs() {

        return new ArrayList<>(langs.keySet());
    }

    public static boolean isSupported(String _value) {
        JSONArray names = langs.names();
        for (int i = 0; i < names.length(); i++) {
            if (_value.equalsIgnoreCase(names.getString(i)))
                return true;
        }
        return false;
    }

    public static String getValidLang(String _value) {
        JSONArray names = langs.names();
        for (int i = 0; i < names.length(); i++) {
            if (_value.equalsIgnoreCase(names.getString(i)))
                return names.getString(i);
        }
        return "ENGLISH";
    }

    public static String getMessage(String key, GuildSettings settings) {
        JSONObject messages;

        if (langs.has(settings.getLang()))
            messages = langs.getJSONObject(settings.getLang());
        else
            messages = langs.getJSONObject("ENGLISH");

        if (messages.has(key))
            return messages.getString(key).replace("%lb%", "\n");
        else
            return "***FAILSAFE MESSAGE*** MESSAGE NOT FOUND!! Message requested: " + key;
    }

    public static String getMessage(String key, String var, String replace, GuildSettings settings) {
        JSONObject messages;

        if (langs.has(settings.getLang()))
            messages = langs.getJSONObject(settings.getLang());
        else
            messages = langs.getJSONObject("ENGLISH");

        if (messages.has(key))
            return messages.getString(key).replace(var, replace).replace("%lb%", "\n");
        else
            return "***FAILSAFE MESSAGE*** MESSAGE NOT FOUND!! Message requested: " + key;
    }
}
