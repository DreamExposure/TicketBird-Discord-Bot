package org.dreamexposure.ticketbird.conf

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.mysql.cj.jdbc.MysqlDataSource
import discord4j.common.JacksonResources
import discord4j.common.store.Store
import discord4j.common.store.legacy.LegacyStoreLayout
import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.`object`.presence.ClientActivity
import discord4j.core.`object`.presence.ClientPresence
import discord4j.core.shard.MemberRequestFilter
import discord4j.core.shard.ShardingStrategy
import discord4j.discordjson.json.GuildData
import discord4j.discordjson.json.MessageData
import discord4j.gateway.intent.Intent
import discord4j.gateway.intent.IntentSet
import discord4j.rest.RestClient
import discord4j.store.api.mapping.MappingStoreService
import discord4j.store.api.service.StoreService
import discord4j.store.jdk.JdkStoreService
import discord4j.store.redis.RedisStoreService
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import org.dreamexposure.ticketbird.TicketBird
import org.dreamexposure.ticketbird.database.DatabaseManager
import org.dreamexposure.ticketbird.listeners.ReadyEventListener
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.utils.GlobalVars
import org.springframework.boot.web.server.ConfigurableWebServerFactory
import org.springframework.boot.web.server.ErrorPage
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.ViewResolverRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse
import org.thymeleaf.spring5.ISpringWebFluxTemplateEngine
import org.thymeleaf.spring5.SpringWebFluxTemplateEngine
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver
import org.thymeleaf.spring5.view.reactive.ThymeleafReactiveViewResolver
import org.thymeleaf.templatemode.TemplateMode
import javax.annotation.PreDestroy
import javax.sql.DataSource

@Configuration
@EnableWebFlux
class WebFluxConfig : WebServerFactoryCustomizer<ConfigurableWebServerFactory>, ApplicationContextAware, WebFluxConfigurer {

    private var ctx: ApplicationContext? = null

    override fun customize(factory: ConfigurableWebServerFactory?) {
        factory?.addErrorPages(ErrorPage(HttpStatus.NOT_FOUND, "/"))
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins("*")
    }

    override fun configureViewResolvers(registry: ViewResolverRegistry) {
        registry.viewResolver(thymeleafChunkedAndDataDrivenResolver())
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        ctx = applicationContext
    }

    @Bean
    fun r2dbcMySqlConnectionPool(): ConnectionFactory {
        return ConnectionFactories.get(ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, "pool")
            .option(ConnectionFactoryOptions.PROTOCOL, "mysql")
            .option(ConnectionFactoryOptions.HOST, BotSettings.SQL_HOST.get())
            .option(ConnectionFactoryOptions.PORT, BotSettings.SQL_PORT.get().toInt())
            .option(ConnectionFactoryOptions.USER, BotSettings.SQL_USER.get())
            .option(ConnectionFactoryOptions.PASSWORD, BotSettings.SQL_PASS.get())
            .option(ConnectionFactoryOptions.DATABASE, BotSettings.SQL_DB.get())
            .option(ConnectionFactoryOptions.SSL, true)
            .build())
    }

    @Bean
    fun jdbcMySqlConnection(): DataSource {
        return MysqlDataSource().apply {
            serverName = BotSettings.SQL_HOST.get()
            port = BotSettings.SQL_PORT.get().toInt()
            user = BotSettings.SQL_USER.get()
            password = BotSettings.SQL_PASS.get()
            sslMode = "REQUIRED"
        }
    }

    @Bean
    fun staticResourceRouter(): RouterFunction<ServerResponse> {
        return RouterFunctions.resources("/**", ClassPathResource("static/"))
    }

    @Bean
    fun thymeleafTemplateResolver(): SpringResourceTemplateResolver {
        val res = SpringResourceTemplateResolver()

        res.setApplicationContext(this.ctx!!)
        res.prefix = "classpath:/templates/"
        res.suffix = ".html"
        res.templateMode = TemplateMode.HTML
        res.isCacheable = false
        res.checkExistence = false

        return res
    }

    @Bean
    fun thymeleafTemplateEngine(): ISpringWebFluxTemplateEngine {
        val templateEngine = SpringWebFluxTemplateEngine()

        templateEngine.addTemplateResolver(thymeleafTemplateResolver())

        return templateEngine
    }

    @Bean
    fun thymeleafChunkedAndDataDrivenResolver(): ThymeleafReactiveViewResolver {
        val viewResolver = ThymeleafReactiveViewResolver()

        viewResolver.templateEngine = thymeleafTemplateEngine()
        viewResolver.responseMaxChunkSizeBytes = 8192

        return viewResolver
    }

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        // Use d4j's object mapper
        return JacksonResources.create().objectMapper.registerKotlinModule()
    }

    @Bean
    fun discordGatewayClient(): GatewayDiscordClient {
        return DiscordClientBuilder.create(BotSettings.TOKEN.get())
            .build().gateway()
            .setEnabledIntents(getIntents())
            .setSharding(getStrategy())
            .setStore(Store.fromLayout(LegacyStoreLayout.of(getStores())))
            .setInitialPresence { ClientPresence.doNotDisturb(ClientActivity.playing("Booting Up!")) }
            .setMemberRequestFilter(MemberRequestFilter.none())
            .withEventDispatcher { it.on(ReadyEvent::class.java, ReadyEventListener::handle) }
            .login()
            .block()!!
    }

    @Bean
    fun discordRestClient(gatewayDiscordClient: GatewayDiscordClient): RestClient {
        return gatewayDiscordClient.restClient
    }


    @PreDestroy
    fun onShutdown(client: GatewayDiscordClient) {
        LOGGER.info(GlobalVars.STATUS, "Shutting down shard")

        DatabaseManager.getManager().disconnectFromMySQL()

        client.logout().subscribe()
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
}
