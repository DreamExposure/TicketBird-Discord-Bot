package org.dreamexposure.ticketbird.file;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.objects.bot.BotSettings;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ReadFile {
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public static Map<String, Map<String, String>> readAllLangFiles() {
        Map<String, Map<String, String>> langs = new HashMap<>();

        try {
            File langDir = new File(BotSettings.LANG_PATH.get());

            for (File f : langDir.listFiles()) {
                // Open the file
                FileReader fr = new FileReader(f);

                Type type = new TypeToken<Map<String, String>>() {
                }.getType();

                Map<String, String> map = new Gson().fromJson(fr, type);
                langs.put(map.get("Language"), map);

                fr.close();
            }
        } catch (Exception e) {
            Logger.getLogger().exception(null, "Failed to load lang files!", e, ReadFile.class);
        }
        return langs;
    }
}