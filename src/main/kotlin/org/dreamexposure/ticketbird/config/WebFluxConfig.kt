package org.dreamexposure.ticketbird.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import discord4j.common.JacksonResources
import discord4j.common.store.Store
import discord4j.common.store.legacy.LegacyStoreLayout
import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.Event
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
import discord4j.store.redis.RedisClusterStoreService
import discord4j.store.redis.RedisStoreService
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.cluster.RedisClusterClient
import kotlinx.coroutines.reactor.mono
import org.dreamexposure.ticketbird.TicketBird
import org.dreamexposure.ticketbird.business.cache.JdkCacheRepository
import org.dreamexposure.ticketbird.listeners.EventListener
import org.dreamexposure.ticketbird.mapper.SnowflakeMapper
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.`object`.Project
import org.dreamexposure.ticketbird.`object`.Ticket
import org.dreamexposure.ticketbird.`object`.TicketCreateState
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.server.ConfigurableWebServerFactory
import org.springframework.boot.web.server.ErrorPage
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
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
import reactor.kotlin.core.publisher.toFlux
import java.time.Duration


@Configuration
@EnableWebFlux
class WebFluxConfig : WebServerFactoryCustomizer<ConfigurableWebServerFactory>, ApplicationContextAware, WebFluxConfigurer {
    private var ctx: ApplicationContext? = null

    // Web stuff
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

    // Discord
    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        // Use d4j's object mapper
        return JacksonResources.create().objectMapper
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .registerModule(SnowflakeMapper())
    }

    @Bean
    fun discordGatewayClient(
        @Value("\${bot.secret.token}") token: String,
        listeners: List<EventListener<*>>,
        stores: StoreService
    ): GatewayDiscordClient {
        return DiscordClientBuilder.create(token)
            .build().gateway()
            .setEnabledIntents(getIntents())
            .setSharding(getStrategy())
            .setStore(Store.fromLayout(LegacyStoreLayout.of(stores)))
            .setInitialPresence { ClientPresence.doNotDisturb(ClientActivity.playing("Booting Up!")) }
            .setMemberRequestFilter(MemberRequestFilter.none())
            .withEventDispatcher { dispatcher ->
                @Suppress("UNCHECKED_CAST")
                (listeners as Iterable<EventListener<Event>>).toFlux()
                    .flatMap {
                        dispatcher.on(it.genericType) { event -> mono { it.handle(event) } }
                    }
            }
            .login()
            .block()!!
    }

    @Bean
    fun discordRestClient(gatewayDiscordClient: GatewayDiscordClient): RestClient {
        return gatewayDiscordClient.restClient
    }

    private fun getStrategy(): ShardingStrategy {
        return ShardingStrategy.builder()
            .count(TicketBird.getShardCount())
            .indices(TicketBird.getShardIndex())
            .build()
    }

    @Bean
    fun discordStores(
        @Value("\${bot.cache.redis:false}") useRedis: Boolean,
        @Value("\${spring.redis.host:null}") redisHost: String?,
        @Value("\${spring.redis.port:null}") redisPort: String?,
        @Value("\${spring.redis.password:null}") redisPassword: CharSequence?,
        @Value("\${redis.cluster:false}") redisCluster: Boolean,
    ): StoreService {
        return if (useRedis) {
            val uriBuilder = RedisURI.Builder
                .redis(redisHost, redisPort!!.toInt())
            if (redisPassword != null) uriBuilder.withPassword(redisPassword)

            val rss = if (redisCluster) {
                RedisClusterStoreService.Builder()
                    .redisClient(RedisClusterClient.create(uriBuilder.build()))
                    .build()
            } else {
                RedisStoreService.Builder()
                    .redisClient(RedisClient.create(uriBuilder.build()))
                    .build()
            }


            MappingStoreService.create()
                .setMappings(rss, GuildData::class.java, MessageData::class.java)
                .setFallback(JdkStoreService())
        } else JdkStoreService()
    }

    private fun getIntents() = IntentSet.of(
        Intent.GUILDS,
        Intent.GUILD_MESSAGES,
        Intent.GUILD_MESSAGE_REACTIONS,
        Intent.DIRECT_MESSAGES,
        Intent.DIRECT_MESSAGE_REACTIONS
    )

    // Cache
    @Bean
    @ConditionalOnProperty("bot.cache.redis", havingValue = "true")
    fun redisCache(
        connection: RedisConnectionFactory,
        @Value("\${bot.cache.prefix}") prefix: String,
        @Value("\${bot.cache.ttl-minutes.settings:60}") settings: Long,
        @Value("\${bot.cache.ttl-minutes.ticket:60}") ticket: Long,
        @Value("\${bot.cache.ttl-minutes.project:120}") project: Long,
        @Value("\${bot.cache.ttl-minutes.ticket-create-state:15}") ticketCreateState: Long,
    ): RedisCacheManager {
        return RedisCacheManager.builder(connection)
            .withCacheConfiguration("$prefix.settingsCache",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(settings))
            ).withCacheConfiguration("$prefix.ticketCache",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(ticket))
            ).withCacheConfiguration("$prefix.projectCache",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(project))
            ).withCacheConfiguration("$prefix.ticketCreateStateCache",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(ticketCreateState)))
            .build()
    }

    @Bean
    fun guidSettingsFallbackCache(@Value("\${bot.cache.ttl-minutes.settings:60}") minutes: Long) =
        JdkCacheRepository<Long, GuildSettings>(Duration.ofMinutes(minutes))

    @Bean
    fun ticketFallbackCache(@Value("\${bot.cache.ttl-minutes.ticket:60}") minutes: Long) =
        JdkCacheRepository<Long, List<Ticket>>(Duration.ofMinutes(minutes))

    @Bean
    fun projectFallbackCache(@Value("\${bot.cache.ttl-minutes.project:120}") minutes: Long) =
        JdkCacheRepository<Long, List<Project>>(Duration.ofMinutes(minutes))

    @Bean
    fun ticketCreateStateCache(@Value("\${bot.cache.ttl-minutes.ticket-create-state:15}") minutes: Long) =
        JdkCacheRepository<String, TicketCreateState>(Duration.ofMinutes(minutes))
}
