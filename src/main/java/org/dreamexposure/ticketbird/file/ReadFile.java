package org.dreamexposure.ticketbird.file;

import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.objects.bot.BotSettings;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ReadFile {
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public static JSONObject readAllLangFiles() {
        JSONObject langs = new JSONObject();

        try {
            File langDir = new File(BotSettings.LANG_PATH.get());

            for (File f : langDir.listFiles()) {
                // Open the file
                FileReader fr = new FileReader(f);

                byte[] encoded = Files.readAllBytes(Paths.get(f.getAbsolutePath()));
                String contents = new String(encoded, StandardCharsets.UTF_8);

                JSONObject json = new JSONObject(contents);

                langs.put(json.getString("Language"), json);

                fr.close();
            }
        } catch (Exception e) {
            Logger.getLogger().exception(null, "Failed to load lang files!", e, ReadFile.class);
        }
        return langs;
    }
}