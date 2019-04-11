package org.dreamexposure.ticketbird;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.data.stored.*;
import discord4j.store.api.mapping.MappingStoreService;
import discord4j.store.jdk.JdkStoreService;
import discord4j.store.redis.RedisStoreService;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.dreamexposure.ticketbird.database.DatabaseManager;
import org.dreamexposure.ticketbird.listeners.ReadyEventListener;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.message.MessageManager;
import org.dreamexposure.ticketbird.module.command.*;
import org.dreamexposure.ticketbird.objects.bot.BotSettings;
import org.dreamexposure.ticketbird.web.spring.SpringController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

@SpringBootApplication
public class Main {
    private static DiscordClient client;

    public static void main(String[] args) throws IOException {
        //Get bot settings
        Properties p = new Properties();
        p.load(new FileReader(new File("settings.properties")));
        BotSettings.init(p);

        Logger.getLogger().init();

        client = createClient();
        if (getClient() == null)
            throw new NullPointerException("Failed to build! Client cannot be null!");

        //Register discord events
        client.getEventDispatcher().on(ReadyEvent.class).subscribe(ReadyEventListener::handle);

        //Connect to MySQL server
        DatabaseManager.getManager().connectToMySQL();
        DatabaseManager.getManager().createTables();

        //Start Spring (catch any issues from it so only the site goes down without affecting bot....
        if (BotSettings.RUN_API.get().equalsIgnoreCase("true")) {
            try {
                SpringController.makeModel();
                SpringApplication.run(Main.class, args);
            } catch (Exception e) {
                Logger.getLogger().exception(null, "'Spring ERROR' by 'PANIC! AT THE WEBSITE'", e, true, Main.class);
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
        MessageManager.reloadLangs();

        client.login().block();
    }

    private static DiscordClient createClient() {
        DiscordClientBuilder clientBuilder = new DiscordClientBuilder(BotSettings.TOKEN.get());
        //Handle shard count and index for multiple java instances
        clientBuilder.setShardIndex(Integer.valueOf(BotSettings.SHARD_INDEX.get()));
        clientBuilder.setShardCount(Integer.valueOf(BotSettings.SHARD_COUNT.get()));


        //Redis info + store service for caching
        if (BotSettings.USE_REDIS_STORES.get().equalsIgnoreCase("true")) {
            RedisURI uri = RedisURI.Builder
                    .redis(BotSettings.REDIS_HOSTNAME.get(), Integer.valueOf(BotSettings.REDIS_PORT.get()))
                    .withPassword(BotSettings.REDIS_PASSWORD.get())
                    .build();

            RedisStoreService rss = new RedisStoreService(RedisClient.create(uri));

            MappingStoreService mapping = MappingStoreService.create()
                    .setMapping(MessageBean.class, rss)
                    .setMapping(ChannelBean.class, rss)
                    .setMapping(TextChannelBean.class, rss)
                    .setMapping(CategoryBean.class, rss)
                    .setMapping(GuildBean.class, rss)
                    .setFallback(new JdkStoreService());

            clientBuilder.setStoreService(mapping);
        }

        return clientBuilder.build();
    }

    //Public stuffs
    public static DiscordClient getClient() {
        return client;
    }
}