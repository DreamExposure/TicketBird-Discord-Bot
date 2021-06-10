package org.dreamexposure.ticketbird.conf

import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import org.dreamexposure.ticketbird.objects.bot.BotSettings
import org.springframework.boot.web.server.ConfigurableWebServerFactory
import org.springframework.boot.web.server.ErrorPage
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
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

@Configuration
@EnableWebFlux
class WebFluxConfig : WebServerFactoryCustomizer<ConfigurableWebServerFactory>,
        ApplicationContextAware, WebFluxConfigurer {

    private var ctx: ApplicationContext? = null

    override fun customize(factory: ConfigurableWebServerFactory?) {
        factory?.addErrorPages(ErrorPage(HttpStatus.NOT_FOUND, "/"))
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
    }

    @Bean
    fun redisConnectionFactory(): LettuceConnectionFactory {
        val rsc = RedisStandaloneConfiguration()
        rsc.hostName = BotSettings.REDIS_HOSTNAME.get()
        rsc.port = BotSettings.REDIS_PORT.get().toInt()
        rsc.password = RedisPassword.of(BotSettings.REDIS_PASSWORD.get())

        return LettuceConnectionFactory(rsc)
    }

    @Bean
    fun mysqlConnectionFactory(): ConnectionFactory {
        return ConnectionFactories.get(ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, "pool")
                .option(ConnectionFactoryOptions.PROTOCOL, "mysql")
                .option(ConnectionFactoryOptions.HOST, BotSettings.SQL_HOST.get())
                .option(ConnectionFactoryOptions.PORT, BotSettings.SQL_PORT.get().toInt())
                .option(ConnectionFactoryOptions.USER, BotSettings.SQL_USER.get())
                .option(ConnectionFactoryOptions.PASSWORD, BotSettings.SQL_PASS.get())
                .option(ConnectionFactoryOptions.DATABASE, BotSettings.SQL_DB.get())
                .build())
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

    override fun configureViewResolvers(registry: ViewResolverRegistry) {
        registry.viewResolver(thymeleafChunkedAndDataDrivenResolver())
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        ctx = applicationContext
    }
}
