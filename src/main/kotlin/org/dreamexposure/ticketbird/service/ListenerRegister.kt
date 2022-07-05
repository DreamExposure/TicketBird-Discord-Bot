package org.dreamexposure.ticketbird.service

import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent
import org.dreamexposure.ticketbird.module.command.CommandExecutor
import org.dreamexposure.ticketbird.module.command.CommandListener
import org.dreamexposure.ticketbird.module.command.DevCommand
import org.dreamexposure.ticketbird.module.command.HelpCommand
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
@Deprecated(message = "Use slash commands")
class ListenerRegister(private val client: GatewayDiscordClient): ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        val onCommand = client.on(MessageCreateEvent::class.java)
            .map(CommandListener::onMessageEvent)
            .then()

        //Register commands.
        val executor = CommandExecutor.getExecutor()
        executor.registerCommand(HelpCommand())
        executor.registerCommand(DevCommand())

        Mono.`when`(onCommand).subscribe()
    }
}
