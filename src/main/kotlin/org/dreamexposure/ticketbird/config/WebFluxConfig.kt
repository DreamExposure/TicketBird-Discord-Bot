package org.dreamexposure.ticketbird.config

import org.springframework.boot.web.server.ConfigurableWebServerFactory
import org.springframework.boot.web.server.ErrorPage
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
}
