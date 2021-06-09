package org.dreamexposure.ticketbird.file;

import org.dreamexposure.ticketbird.logger.Logger;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ReadFile {
    public static JSONObject readAllLangFiles() {
        JSONObject langs = new JSONObject();

        try {
            for (File f : getResourceFolderFiles("langs")) {
                // Open the file
                FileReader fr = new FileReader(f);

                byte[] encoded = Files.readAllBytes(Paths.get(f.getAbsolutePath()));
                String contents = new String(encoded, StandardCharsets.UTF_8);

                JSONObject json = new JSONObject(contents);

                langs.put(json.getString("Language"), json);

                fr.close();
            }
        } catch (Exception e) {
            Logger.getLogger().exception(null, "Failed to load lang files!", e, true, ReadFile.class);
        }
        return langs;
    }

    private static File[] getResourceFolderFiles(String folder) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(folder);
        String path = url.getPath();
        return new File(path).listFiles();
    }
}
