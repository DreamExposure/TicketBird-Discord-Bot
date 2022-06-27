package org.dreamexposure.ticketbird.file;

import org.dreamexposure.ticketbird.utils.GlobalVars;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Deprecated
public class ReadFile {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadFile.class);

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
            LOGGER.error(GlobalVars.INSTANCE.getDEFAULT(), "Failed to load lang files!", e);
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
