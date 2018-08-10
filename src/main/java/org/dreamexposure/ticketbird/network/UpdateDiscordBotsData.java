package org.dreamexposure.ticketbird.network;

import org.discordbots.api.client.DiscordBotListAPI;
import org.dreamexposure.ticketbird.Main;
import org.dreamexposure.ticketbird.objects.bot.BotSettings;

import java.util.Timer;
import java.util.TimerTask;

public class UpdateDiscordBotsData {
    private static DiscordBotListAPI api;
    private static Timer timer;

    public static void init() {
        if (BotSettings.UPDATE_SITES.get().equalsIgnoreCase("true")) {

            api = new DiscordBotListAPI.Builder().token(BotSettings.DBO_TOKEN.get()).build();

            timer = new Timer(true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    updateStats();
                }
            }, 60 * 60 * 1000);
        }
    }

    public static void shutdown() {
        if (timer != null)
            timer.cancel();
    }

    private static void updateStats() {
        if (api != null)
            api.setStats(BotSettings.ID.get(), Main.getClient().getGuilds().size());
    }
}