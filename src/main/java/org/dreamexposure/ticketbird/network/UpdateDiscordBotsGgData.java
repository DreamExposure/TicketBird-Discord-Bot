package org.dreamexposure.ticketbird.network;

import okhttp3.*;
import org.dreamexposure.ticketbird.Main;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.objects.bot.BotSettings;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class UpdateDiscordBotsGgData {
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

    @SuppressWarnings("ConstantConditions")
    private static void updateSiteBotMeta() {
        try {
            int serverCount = Main.getClient().getGuilds().count().block().intValue();

            JSONObject json = new JSONObject()
                    .put("guildCount", serverCount)
                    .put("shardCount", Main.getShardCount());

            OkHttpClient client = new OkHttpClient();

            RequestBody body = RequestBody.create(JSON, json.toString());
            Request request = new Request.Builder()
                    .url("https://discord.bots.gg/api/v1/bots/456140067220750336/stats")
                    .post(body)
                    .header("Authorization", BotSettings.GG_TOKEN.get())
                    .header("Content-Type", "application/json")
                    .build();

            Response response = client.newCall(request).execute();

            if (response.code() == 200)
                Logger.getLogger().debug("Successfully updated Discord Bots.gg List!", false);
        } catch (Exception e) {
            //Handle issue.
            System.out.println("Failed to update Discord Bots.gg list metadata!");
            Logger.getLogger().exception(null, "Failed to update Discord Bots.gg list.", e, true, UpdateDiscordBotsGgData.class);
            e.printStackTrace();
        }
    }
}
