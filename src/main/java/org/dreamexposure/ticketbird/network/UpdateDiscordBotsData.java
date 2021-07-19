package org.dreamexposure.ticketbird.network;

import discord4j.core.GatewayDiscordClient;
import org.discordbots.api.client.DiscordBotListAPI;
import org.dreamexposure.ticketbird.objects.bot.BotSettings;

import java.util.Timer;
import java.util.TimerTask;

public class UpdateDiscordBotsData {
    private static DiscordBotListAPI api;
    private static Timer timer;

    private static GatewayDiscordClient client;

    public static void init(GatewayDiscordClient client) {
        if (BotSettings.UPDATE_SITES.get().equalsIgnoreCase("true")) {
            UpdateDiscordBotsData.client = client;

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
        try {
            if (api != null)
                api.setStats(BotSettings.ID.get(), client.getGuilds().count().block().intValue());
        } catch (Exception ignore) {}
    }
}
