package org.dreamexposure.ticketbird.service

import discord4j.common.JacksonResources
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.rest.RestClient
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component

@Component
class GlobalCommandRegistrar(
    private val restClient: RestClient
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        val d4jMapper = JacksonResources.create()

        val matcher = PathMatchingResourcePatternResolver()
        val applicationService = restClient.applicationService
        val applicationId = restClient.applicationId.block()!!

        val commands = mutableListOf<ApplicationCommandRequest>()
        for (res in matcher.getResources("commands/*.json")) {
            val request = d4jMapper.objectMapper.readValue(res.toString(), ApplicationCommandRequest::class.java)
            commands.add(request)
        }

        applicationService.bulkOverwriteGlobalApplicationCommand(applicationId, commands).subscribe()
    }
}
