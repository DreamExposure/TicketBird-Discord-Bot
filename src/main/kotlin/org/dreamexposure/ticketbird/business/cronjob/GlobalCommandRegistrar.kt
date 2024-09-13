package org.dreamexposure.ticketbird.business.cronjob

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import discord4j.core.DiscordClient
import discord4j.discordjson.json.ApplicationCommandRequest
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.utils.GlobalVars.DEFAULT
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component

@Component
class GlobalCommandRegistrar(
    private val discordClient: DiscordClient,
    private val objectMapper: ObjectMapper,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        val matcher = PathMatchingResourcePatternResolver()
        val applicationService = discordClient.applicationService
        val applicationId = discordClient.applicationId.block()!!

        val commands = mutableListOf<ApplicationCommandRequest>()
        for (res in matcher.getResources("commands/global/*.json")) {
            val request = objectMapper.readValue<ApplicationCommandRequest>(res.inputStream)
            commands.add(request)
        }

        applicationService.bulkOverwriteGlobalApplicationCommand(applicationId, commands)
            .doOnNext { LOGGER.debug("Bulk overwrite read: ${it.name()}") }
            .doOnError { LOGGER.error(DEFAULT, "Bulk overwrite failed", it) }
            .subscribe()
    }
}
