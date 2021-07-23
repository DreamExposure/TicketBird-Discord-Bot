package org.dreamexposure.ticketbird.service;

import discord4j.core.GatewayDiscordClient;
import org.dreamexposure.ticketbird.module.status.StatusChanger;
import org.dreamexposure.ticketbird.network.UpdateDiscordBotsData;
import org.dreamexposure.ticketbird.network.UpdateDiscordBotsGgData;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Timer;

public class TimeManager {
    private static TimeManager instance;

    private final ArrayList<Timer> timers = new ArrayList<>();

    private TimeManager() {
    } //Prevent initialization

    /**
     * Gets the instance of the TimeManager that is loaded.
     *
     * @return The instance of the TimeManager.
     */
    public static TimeManager getManager() {
        if (instance == null) {
            instance = new TimeManager();
        }
        return instance;
    }

    /**
     * Initializes the TimeManager and schedules the appropriate Timers.
     */
    public void init(GatewayDiscordClient client) {
        Timer timer = new Timer(true);
        timer.schedule(new StatusChanger(client), Duration.ofMinutes(1).toMillis(), Duration.ofMinutes(5).toMillis());

        timers.add(timer);

        Timer amt = new Timer(true);
        amt.schedule(new ActivityMonitor(client), Duration.ofHours(1).toMillis(), Duration.ofHours(1).toMillis());

        timers.add(amt);

        //Start the bot site updates
        UpdateDiscordBotsData.init(client);
        UpdateDiscordBotsGgData.init(client);
    }

    /**
     * Gracefully shuts down the TimeManager and exits all timer threads preventing errors.
     */
    public void shutdown() {
        for (Timer t : timers) {
            t.cancel();
        }
    }
}
