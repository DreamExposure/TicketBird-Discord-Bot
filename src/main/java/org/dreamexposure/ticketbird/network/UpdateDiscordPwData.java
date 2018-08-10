package org.dreamexposure.ticketbird.network;

import okhttp3.*;
import org.dreamexposure.ticketbird.Main;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.objects.bot.BotSettings;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class UpdateDiscordPwData {
    private static Timer timer;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static void init() {
        if (BotSettings.UPDATE_SITES.get().equalsIgnoreCase("true")) {
            timer = new Timer(true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    updateSiteBotMeta();
                }
            }, 60 * 60 * 1000);
        }
    }

    public static void shutdown() {
        if (timer != null)
            timer.cancel();
    }

    private static void updateSiteBotMeta() {
        try {
            int serverCount = Main.getClient().getGuilds().size();

            JSONObject json = new JSONObject().put("server_count", serverCount);

            OkHttpClient client = new OkHttpClient();

            RequestBody body = RequestBody.create(JSON, json.toString());
            Request request = new Request.Builder()
                    .url("https://bots.discord.pw/api/bots/456140067220750336/stats")
                    .post(body)
                    .header("Authorization", BotSettings.PW_TOKEN.get())
                    .header("Content-Type", "application/json")
                    .build();

            Response response = client.newCall(request).execute();

            if (response.code() == 200)
                Logger.getLogger().debug("Successfully updated Discord PW List!");
        } catch (Exception e) {
            //Handle issue.
            System.out.println("Failed to update Discord PW list metadata!");
            Logger.getLogger().exception(null, "Failed to update Discord PW list.", e, UpdateDiscordPwData.class);
            e.printStackTrace();
        }
    }
}