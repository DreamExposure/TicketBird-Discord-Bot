package org.dreamexposure.ticketbird

import discord4j.common.store.Store
import discord4j.common.store.legacy.LegacyStoreLayout
import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.presence.ClientActivity
import discord4j.core.`object`.presence.ClientPresence
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.shard.ShardingStrategy
import discord4j.discordjson.json.GuildData
import discord4j.discordjson.json.MessageData
import discord4j.gateway.intent.Intent
import discord4j.gateway.intent.IntentSet
import discord4j.store.api.mapping.MappingStoreService
import discord4j.store.api.service.StoreService
import discord4j.store.jdk.JdkStoreService
import discord4j.store.redis.RedisStoreService
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import org.dreamexposure.ticketbird.database.DatabaseManager
import org.dreamexposure.ticketbird.listeners.ReadyEventListener
import org.dreamexposure.ticketbird.logger.Logger
import org.dreamexposure.ticketbird.message.MessageManager
import org.dreamexposure.ticketbird.module.command.*
import org.dreamexposure.ticketbird.network.UpdateDiscordBotsData
import org.dreamexposure.ticketbird.network.UpdateDiscordBotsGgData
import org.dreamexposure.ticketbird.objects.bot.BotSettings
import org.dreamexposure.ticketbird.service.TimeManager
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.io.FileReader
import java.util.*
import javax.annotation.PreDestroy

@Component
@SpringBootApplication(exclude = [SessionAutoConfiguration::class])
class TicketBird {
    companion object {
        private lateinit var client: GatewayDiscordClient

        @JvmStatic
        fun getShardIndex(): Int {
            /*
            This fucking sucks. So k8s doesn't expose the pod ordinal for a pod in a stateful set
            https://github.com/kubernetes/kubernetes/pull/68719

            This has been an open issue and PR for over 3 years now, and has gone stale as of March 3rd 2021.

            So, in order to get the pod ordinal since its not directly exposed, we have to get the hostname, and parse
            the ordinal out of that.

            To make sure we don't use this when running anywhere but inside of k8s, we are mapping the hostname to an env
            variable SHARD_POD_NAME and if that is present, parsing it for the pod ordinal to tell the bot its shard index.

            This will be removed when/if they add this feature directly and SHARD_INDEX will be an env variable...
            */

            //Check if we are running in k8s or not...
            val shardPodName = System.getenv("SHARD_POD_NAME")
            return if (shardPodName != null) {
                //In k8s, parse this shit

                //Pod name would look like `ticketbird-dev-N` where N is the ordinal and therefore shard index.
                val parts = shardPodName.split("-").toTypedArray()
                parts[parts.size - 1].toInt() //Get last part in name, that should be ordinal
            } else {
                //Fall back to config value
                BotSettings.SHARD_INDEX.get().toInt()
            }
        }

        @JvmStatic
        fun getShardCount(): Int {
            val shardCount = System.getenv("SHARD_COUNT")
            return shardCount?.toInt() ?: //Fall back to config
            BotSettings.SHARD_COUNT.get().toInt()
        }

        @JvmStatic
        fun main(args: Array<String>) {
            //Get settings
            val p = Properties()
            p.load(FileReader("settings.properties"))
            BotSettings.init(p)

            Logger.getLogger().init()

            DatabaseManager.getManager().connectToMySQL()
            DatabaseManager.getManager().handleMigrations()

            //Start spring
            try {
                val app = SpringApplication(TicketBird::class.java)
                app.setAdditionalProfiles(BotSettings.PROFILE.get())
                app.run(*args)
            } catch (e: Exception) {
                e.printStackTrace()
                Logger.getLogger().exception(null, "Spring error!", e, true, TicketBird::class.java)
            }

            //Load language files.
            MessageManager.reloadLangs()

            DiscordClientBuilder.create(BotSettings.TOKEN.get())
                    .build().gateway()
                    .setEnabledIntents(getIntents())
                    .setSharding(getStrategy())
                    .setStore(Store.fromLayout(LegacyStoreLayout.of(getStores())))
                    .setInitialPresence { ClientPresence.doNotDisturb(ClientActivity.playing("Booting Up!")) }
                    .withGateway { client ->
                        TicketBird.client = client

                        val onReady = client.on(ReadyEvent::class.java, ReadyEventListener::handle)
                                .then()

                        val onCommand = client.on(MessageCreateEvent::class.java)
                                .map(CommandListener::onMessageEvent)
                                .then()

                        //Register commands.
                        val executor = CommandExecutor.getExecutor()
                        executor.registerCommand(TicketBirdCommand())
                        executor.registerCommand(ProjectCommand())
                        executor.registerCommand(CloseCommand())
                        executor.registerCommand(HoldCommand())
                        executor.registerCommand(HelpCommand())
                        executor.registerCommand(DevCommand())

                        return@withGateway Mono.`when`(onReady, onCommand)
                    }.block()
        }
    }

    @PreDestroy
    fun onShutdown() {
        Logger.getLogger().status("Shutting down shard", "Shutting down shard")

        TimeManager.getManager().shutdown()
        DatabaseManager.getManager().disconnectFromMySQL()

        UpdateDiscordBotsData.shutdown()
        UpdateDiscordBotsGgData.shutdown()

        client.logout().subscribe()
    }
}


private fun getStrategy(): ShardingStrategy {
    return ShardingStrategy.builder()
            .count(TicketBird.getShardCount())
            .indices(TicketBird.getShardIndex())
            .build()
}

private fun getStores(): StoreService {
    return if (BotSettings.USE_REDIS_STORES.get().equals("true", ignoreCase = true)) {
        val uri = RedisURI.Builder
                .redis(BotSettings.REDIS_HOSTNAME.get(), BotSettings.REDIS_PORT.get().toInt())
                //.withPassword(BotSettings.REDIS_PASSWORD.get())
                .build()

        val rss = RedisStoreService.Builder()
                .redisClient(RedisClient.create(uri))
                .build()

        MappingStoreService.create()
                .setMappings(rss, GuildData::class.java, MessageData::class.java)
                .setFallback(JdkStoreService())
    } else JdkStoreService()
}

private fun getIntents(): IntentSet {
    val default = IntentSet.of(
            Intent.GUILDS,
            Intent.GUILD_MESSAGES,
            Intent.GUILD_MESSAGE_REACTIONS,
            Intent.DIRECT_MESSAGES,
            Intent.DIRECT_MESSAGE_REACTIONS
    )

    return if (BotSettings.USE_SPECIAL_INTENTS.get().equals("true", true)) {
        default.or(IntentSet.of(Intent.GUILD_MEMBERS))
    } else default
}
