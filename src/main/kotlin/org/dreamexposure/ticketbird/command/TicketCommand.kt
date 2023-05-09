package org.dreamexposure.ticketbird.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.TextChannel
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.*
import org.dreamexposure.ticketbird.business.cache.CacheRepository
import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.extensions.asSeconds
import org.dreamexposure.ticketbird.extensions.discord4j.deleteFollowupDelayed
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.`object`.TicketCreateState
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrNull

@Component
class TicketCommand(
    private val projectService: ProjectService,
    private val ticketService: TicketService,
    private val localeService: LocaleService,
    private val componentService: ComponentService,
    private val staticMessageService: StaticMessageService,
    private val ticketCreateStateCache: CacheRepository<String, TicketCreateState>,
) : SlashCommand {
    override val name = "ticket"
    override val ephemeral = true

    private val genericMessageDeleteSeconds = Config.TIMING_MESSAGE_DELETE_GENERIC_SECONDS.getLong().asSeconds()
    private val openTicketMessageDeleteSeconds = Config.TIMING_MESSAGE_DELETE_TICKET_FLOW_SECONDS.getLong().asSeconds()

    override suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings) {
        when (event.options[0].name) {
            "open" -> open(event, settings)
            "topic" -> topic(event, settings)
            "add" -> add(event, settings)
            "remove" -> remove(event, settings)
            "hold" -> hold(event, settings)
            "close" -> close(event, settings)
            "checksum" -> checksum(event, settings)
        }
    }

    private suspend fun open(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val info = event.options[0].getOption("info")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .orElse("")
        val topicId = event.options[0].getOption("topic")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .map(String::toLong)
            .orElse(-1)

        // Check if ticket bird is even functional
        if (!settings.hasRequiredIdsSet() && !settings.requiresRepair) {
            // TicketBird never init
            event.createFollowup(localeService.getString(settings.locale, "generic.not-init"))
                .withEphemeral(true)
                .awaitSingle()
            return
        }
        if (settings.requiresRepair) {
            // TicketBird broken, needs repair
            event.createFollowup(localeService.getString(settings.locale, "generic.repair-required"))
                .withEphemeral(true)
                .awaitSingle()
            return
        }

        // Check if project required but missing; if so; cache info, give them project dropdown
        if (settings.useProjects && topicId <= 0) {
            ticketCreateStateCache.put("${settings.guildId}.${event.interaction.user.id.asLong()}", TicketCreateState(ticketInfo = info))

            event.createFollowup(localeService.getString(settings.locale, "dropdown.select-project.prompt"))
                .withComponents(*componentService.getProjectSelectComponents(settings, withCreate = true))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, openTicketMessageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        // Only get projects if using project, otherwise no reason to do the fetch
        val project = if (settings.useProjects) projectService.getProject(settings.guildId, topicId) else null

        // Check if project required and exists; if not; cache info, give them project dropdown
        if (settings.useProjects && project == null) {
            ticketCreateStateCache.put("${settings.guildId}.${event.interaction.user.id.asLong()}", TicketCreateState(ticketInfo = info))

            event.createFollowup(localeService.getString(settings.locale, "command.support.topic.not-found"))
                .withComponents(*componentService.getProjectSelectComponents(settings, withCreate = true))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, openTicketMessageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        // Create ticket
        val ticket = ticketService.createNewTicketFull(
            guildId = settings.guildId,
            creatorId = event.interaction.user.id,
            project = project,
            info = info
        )

        // Respond
        event.createFollowup(localeService.getString(
            settings.locale,
            "generic.success.ticket-open",
            ticket.channel.asString()
        )).withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, openTicketMessageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    private suspend fun topic(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val topicId = event.options[0].getOption("topic")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .map(String::toLong)
            .orElse(-1)
        val ticket = ticketService.getTicket(settings.guildId, event.interaction.channelId)

        // Handle if not in a ticket channel
        if (ticket == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.topic.not-ticket"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, genericMessageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        val project = projectService.getProject(settings.guildId, topicId)

        if (project == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.topic.not-found"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, genericMessageDeleteSeconds) }
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
            .flatMap { event.deleteFollowupDelayed(it, genericMessageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    private suspend fun add(event: ChatInputInteractionEvent, settings: GuildSettings) {
        // TODO: add perms check
        TODO("Not yet implemented")
    }

    private suspend fun remove(event: ChatInputInteractionEvent, settings: GuildSettings) {
        // TODO: add perms check
        TODO("Not yet implemented")
    }

    private suspend fun hold(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val ticket = ticketService.getTicket(settings.guildId, event.interaction.channelId)

        // Handle if not in a ticket channel
        @Suppress("FoldInitializerAndIfToElvis") // Using == null for readability
        if (ticket == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.hold.not-ticket"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, genericMessageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }
        // Handle if ticket is already on hold
        if (ticket.category == settings.holdCategory) {
            event.createFollowup(localeService.getString(settings.locale, "command.hold.already-held"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, genericMessageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        // We can place the ticket on hold now
        ticketService.holdTicket(settings.guildId, event.interaction.channelId)
        staticMessageService.update(settings.guildId)

        event.createFollowup(localeService.getString(settings.locale, "command.hold.success"))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, genericMessageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    private suspend fun close(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val ticket = ticketService.getTicket(settings.guildId, event.interaction.channelId)

        // Handle if not in a ticket channel
        @Suppress("FoldInitializerAndIfToElvis") // Using == null for readability
        if (ticket == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.close.not-ticket"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, genericMessageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }
        // Handle if ticket is already closed
        if (ticket.category == settings.closeCategory) {
            event.createFollowup(localeService.getString(settings.locale, "command.close.already-closed"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, genericMessageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        // We can close the ticket now
        ticketService.closeTicket(settings.guildId, event.interaction.channelId)
        staticMessageService.update(settings.guildId)

        event.createFollowup(localeService.getString(settings.locale, "command.close.success"))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, genericMessageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    private suspend fun checksum(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val transcriptLog = event.options[0].getOption("transcript")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asAttachment)
            .getOrNull()
        val attachmentsZip = event.options[0].getOption("attachments")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asAttachment)
            .getOrNull()


        TODO("Not yet implemented")
    }
}
