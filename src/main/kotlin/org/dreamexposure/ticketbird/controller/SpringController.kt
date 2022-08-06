package org.dreamexposure.ticketbird.controller

import discord4j.rest.RestClient
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.LocalDate

@Controller
class SpringController(private val discordClient: RestClient) {

    private fun getModel(): MutableMap<String, Any> {
        return mutableMapOf(
                "loggedIn" to false,
                "client" to discordClient.applicationId.block()!!,
                "year" to LocalDate.now().year,
        )
    }

    @RequestMapping("/", "/home")
    fun home(model: MutableMap<String, Any>, swe: ServerWebExchange): Mono<String> {
        model.clear()
        model.putAll(getModel())

        return Mono.just("index")
    }

    @RequestMapping("/about")
    fun about(model: MutableMap<String, Any>, swe: ServerWebExchange): Mono<String> {
        model.clear()
        model.putAll(getModel())

        return Mono.just("about")
    }

    @RequestMapping("/commands")
    fun commands(model: MutableMap<String, Any>, swe: ServerWebExchange): Mono<String> {
        model.clear()
        model.putAll(getModel())

        return Mono.just("commands")
    }

    @RequestMapping("/setup")
    fun setup(model: MutableMap<String, Any>, swe: ServerWebExchange): Mono<String> {
        model.clear()
        model.putAll(getModel())

        return Mono.just("setup")
    }

    @RequestMapping("/policy/privacy")
    fun privacyPolicy(model: MutableMap<String, Any>, swe: ServerWebExchange): Mono<String> {
        model.clear()
        model.putAll(getModel())

        return Mono.just("policy/privacy")
    }
}
