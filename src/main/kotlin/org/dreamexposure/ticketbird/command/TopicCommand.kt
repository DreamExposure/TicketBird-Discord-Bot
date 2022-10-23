package org.dreamexposure.ticketbird.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.TextChannel
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.ProjectService
import org.dreamexposure.ticketbird.business.TicketService
import org.dreamexposure.ticketbird.extensions.asSeconds
import org.dreamexposure.ticketbird.extensions.discord4j.deleteFollowupDelayed
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TopicCommand(
    private val ticketService: TicketService,
    private val projectService: ProjectService,
    private val localeService: LocaleService,
    @Value("\${bot.timing.message-delete.generic.seconds:30}")
    private val messageDeleteSeconds: Long,
): SlashCommand {
    override val name = "topic"
    override val ephemeral = true

    override suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val topic = event.getOption("topic")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .orElse("")
        val ticket = ticketService.getTicket(settings.guildId, event.interaction.channelId)

        // Handle if not in a ticket channel
        if (ticket == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.topic.not-ticket"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds.asSeconds()) }
                .awaitSingleOrNull()
            return
        }

        val project = projectService.getProject(settings.guildId, topic)

        if (project == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.topic.not-found"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds.asSeconds()) }
                .awaitSingleOrNull()
            return
        }

        // Update
        event.interaction.channel
            .ofType(TextChannel::class.java)
            .flatMap { it.edit().withName("${project.prefix}-ticket-${ticket.number}") }
            .awaitSingleOrNull()

        ticket.project = project.name
        ticketService.updateTicket(ticket)

        event.createFollowup(localeService.getString(settings.locale, "command.topic.success", project.name))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds.asSeconds()) }
            .awaitSingleOrNull()
    }
}
