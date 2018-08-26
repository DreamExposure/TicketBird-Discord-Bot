package org.dreamexposure.ticketbird;

import org.dreamexposure.ticketbird.database.DatabaseManager;
import org.dreamexposure.ticketbird.listeners.ReadyEventListener;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.message.MessageManager;
import org.dreamexposure.ticketbird.module.command.*;
import org.dreamexposure.ticketbird.objects.bot.BotSettings;
import org.dreamexposure.ticketbird.web.spring.SpringController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.util.DiscordException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

@SpringBootApplication
public class Main {
    private static IDiscordClient client;

    public static void main(String[] args) throws IOException {
        //Get bot settings
        Properties p = new Properties();
        p.load(new FileReader(new File("settings.properties")));
        BotSettings.init(p);

        Logger.getLogger().init();

        client = createClient(BotSettings.TOKEN.get());
        if (getClient() == null)
            throw new NullPointerException("Failed to build! Client cannot be null!");

        //Register events
        EventDispatcher dispatcher = getClient().getDispatcher();
        dispatcher.registerListener(new ReadyEventListener());

        getClient().login();

        //Connect to MySQL server
        DatabaseManager.getManager().connectToMySQL();
        DatabaseManager.getManager().createTables();

        //Start Spring (catch any issues from it so only the site goes down without affecting bot....
        if (BotSettings.RUN_API.get().equalsIgnoreCase("true")) {
            try {
                SpringController.makeModel();
                SpringApplication.run(Main.class, args);
            } catch (Exception e) {
                Logger.getLogger().exception(null, "'Spring ERROR' by 'PANIC! AT THE WEBSITE'", e, Main.class);
            }
        }

        //Register commands.
        CommandExecutor executor = CommandExecutor.getExecutor().enable();
        executor.registerCommand(new TicketBirdCommand());
        executor.registerCommand(new ProjectCommand());
        executor.registerCommand(new CloseCommand());
        executor.registerCommand(new HoldCommand());
        executor.registerCommand(new HelpCommand());
        executor.registerCommand(new DevCommand());

        //Load language files.
        MessageManager.loadLangs();
    }

    public static IDiscordClient getClient() {
        return client;
    }

    private static IDiscordClient createClient(String token) {
        ClientBuilder clientBuilder = new ClientBuilder(); // Creates the ClientBuilder instance
        clientBuilder.withToken(token).withRecommendedShardCount(); // Adds the login info to the builder
        try {
            return clientBuilder.build();
        } catch (DiscordException e) {
            e.printStackTrace();
        }
        return null;
    }
}