package org.dreamexposure.ticketbird.listeners

import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import org.dreamexposure.ticketbird.command.SlashCommand
import org.dreamexposure.ticketbird.database.DatabaseManager
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Component
class SlashCommandListener(val client: GatewayDiscordClient): ApplicationRunner, ApplicationContextAware {
    private var cmds: List<SlashCommand> = listOf()

    fun handle(event: ChatInputInteractionEvent): Mono<Void> {
        if (!event.interaction.guildId.isPresent) {
            return event.reply("Commands not supported in DMs.")
        }

        return Flux.fromIterable(cmds)
            .filter { it.name == event.commandName }
            .next()
            .flatMap { command ->
                val mono =
                    if (command.ephemeral) event.deferReply().withEphemeral(true)
                    else event.deferReply()

                return@flatMap mono.then(Mono.fromCallable {
                    DatabaseManager.getManager().getSettings(event.interaction.guildId.get())
                }.subscribeOn(Schedulers.boundedElastic())).flatMap { settings ->
                    command.handle(event, settings)
                }.switchIfEmpty(event.createFollowup("An unknown error occurred.").withEphemeral(true))
            }.then()
    }

    override fun run(args: ApplicationArguments?) {
        client.on(ChatInputInteractionEvent::class.java, this::handle).subscribe()
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        cmds = applicationContext.getBeansOfType(SlashCommand::class.java).values.toList()
    }
}
