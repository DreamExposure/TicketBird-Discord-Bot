package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent
import discord4j.core.`object`.entity.Attachment
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Permission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.dreamexposure.ticketbird.TicketCreateStateCache
import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.extensions.asSeconds
import org.dreamexposure.ticketbird.extensions.discord4j.deleteFollowupDelayed
import org.dreamexposure.ticketbird.extensions.sha256Hash
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.`object`.Ticket
import org.dreamexposure.ticketbird.`object`.TicketCreateState
import org.dreamexposure.ticketbird.utils.GlobalVars
import org.springframework.stereotype.Component
import java.net.URL
import java.time.Instant

@Component
class InteractionService(
    private val projectService: ProjectService,
    private val ticketService: TicketService,
    private val localeService: LocaleService,
    private val componentService: ComponentService,
    private val staticMessageService: StaticMessageService,
    private val ticketCreateStateCache: TicketCreateStateCache,
) {
    private val openTicketMessageDeleteSeconds = Config.TIMING_MESSAGE_DELETE_TICKET_FLOW_SECONDS.getLong().asSeconds()
    private val genericMessageDeleteSeconds = Config.TIMING_MESSAGE_DELETE_GENERIC_SECONDS.getLong().asSeconds()

    suspend fun openTicketViaInteraction(info: String, topicId: Long, ephemeral: Boolean, event: DeferrableInteractionEvent, settings: GuildSettings) {
        // Check if ticket bird is even functional
        if (!settings.hasRequiredIdsSet() && !settings.requiresRepair) {
            // TicketBird never init
            event.createFollowup(localeService.getString(settings.locale, "generic.not-init"))
                .withEphemeral(ephemeral)
                .awaitSingle()
            return
        }
        if (settings.requiresRepair) {
            // TicketBird broken, needs repair
            event.createFollowup(localeService.getString(settings.locale, "generic.repair-required"))
                .withEphemeral(ephemeral)
                .awaitSingle()
            return
        }

        // Check if project required but missing; if so; cache info, give them project dropdown
        if (settings.useProjects && topicId <= 0) {
            ticketCreateStateCache.put(guildId = settings.guildId, event.interaction.user.id, TicketCreateState(ticketInfo = info))

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
            ticketCreateStateCache.put(guildId = settings.guildId, event.interaction.user.id, TicketCreateState(ticketInfo = info))

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

    suspend fun holdTicketViaInteraction(ephemeral: Boolean, event: DeferrableInteractionEvent, settings: GuildSettings) {
        val ticket = ticketService.getTicket(settings.guildId, event.interaction.channelId)

        // Handle if not in a ticket channel
        if (ticket == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.hold.not-ticket"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, genericMessageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        // Permission check
        if (!handleIfHasLifecyclePermission(ticket, event, settings)) return

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

    suspend fun closeTicketViaInteraction(ephemeral: Boolean, event: DeferrableInteractionEvent, settings: GuildSettings) {
        val ticket = ticketService.getTicket(settings.guildId, event.interaction.channelId)

        // Handle if not in a ticket channel
        if (ticket == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.close.not-ticket"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, genericMessageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        // Permission check
        if (!handleIfHasLifecyclePermission(ticket, event, settings)) return

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

    suspend fun changeTopicViaCommand(topicId: Long, ephemeral: Boolean, event: ChatInputInteractionEvent, settings: GuildSettings) {
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

        // Permission check
        if (!handleIfHasLifecyclePermission(ticket, event, settings)) return

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

    suspend fun addParticipantViaCommand(write: Boolean, userId: Snowflake, ephemeral: Boolean, event: ChatInputInteractionEvent, settings: GuildSettings) {
        val ticket = ticketService.getTicket(settings.guildId, event.interaction.channelId)

        // Handle if not in a ticket channel
        if (ticket == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.ticket.add.not-ticket"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, genericMessageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        // Permission check
        if (!handleIfHasLifecyclePermission(ticket, event, settings)) return

        // Check if user already has access to this ticket
        val canSeeChannel = event.interaction.channel.ofType(TextChannel::class.java)
            .flatMap { it.getEffectivePermissions(userId) }
            .map { it.contains(Permission.VIEW_CHANNEL) || it.contains(Permission.ADMINISTRATOR) }
            .awaitSingle()
        if (canSeeChannel) {
            event.createFollowup(localeService.getString(settings.locale, "command.ticket.add.already"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, genericMessageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        ticketService.addParticipant(settings.guildId, ticket.channel, userId, event.interaction.user.id, write)

        event.createFollowup(localeService.getString(settings.locale, "generic.success"))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, genericMessageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    suspend fun removeParticipantViaCommand(userId: Snowflake, ephemeral: Boolean, event: ChatInputInteractionEvent, settings: GuildSettings) {
        val ticket = ticketService.getTicket(settings.guildId, event.interaction.channelId)

        // Handle if not in a ticket channel
        if (ticket == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.ticket.remove.not-ticket"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, genericMessageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        // Permission check
        if (!handleIfHasLifecyclePermission(ticket, event, settings)) return

        // Check if target user is admin
        val userIsAdmin = event.interaction.channel.ofType(TextChannel::class.java)
            .flatMap { it.getEffectivePermissions(userId) }
            .map { it.contains(Permission.ADMINISTRATOR) }
            .awaitSingle()
        if (userIsAdmin) {
            event.createFollowup(localeService.getString(settings.locale, "command.ticket.remove.admin"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, genericMessageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        ticketService.removeParticipant(settings.guildId, ticket.channel, userId, event.interaction.user.id)

        event.createFollowup(localeService.getString(settings.locale, "generic.success"))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, genericMessageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    suspend fun validateChecksumViaInteraction(file: Attachment, ephemeral: Boolean, event: DeferrableInteractionEvent, settings: GuildSettings) {
        val fileSha =  withContext(Dispatchers.IO) {
                URL(file.url).openStream().use {
                    it.readBytes().sha256Hash()
                }
        }
        val ticket = ticketService.getTicket(settings.guildId, fileSha)

        if (ticket == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.ticket.checksum.no-tickets", fileSha))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, genericMessageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        val embed = EmbedCreateSpec.builder()
            .author(localeService.getString(settings.locale, "bot.name"), null, GlobalVars.iconUrl)
            .color(GlobalVars.embedColor)
            .title(localeService.getString(settings.locale, "embed.checksum.title"))
            .addField(localeService.getString(settings.locale, "embed.checksum.field.checksum"), "`$fileSha`", false)
            .addField(localeService.getString(settings.locale, "embed.checksum.field.ticket"), "ticket-${ticket.number}", false)
            .addField(localeService.getString(settings.locale, "embed.checksum.field.transcript"), "`${ticket.attachmentsSha256 ?: "N/a"}`", true)
            .addField(localeService.getString(settings.locale, "embed.checksum.field.attachments"), "`${ticket.attachmentsSha256 ?: "N/a"}`", true)
            .footer(localeService.getString(settings.locale, "embed.checksum.footer"), null)
            .timestamp(Instant.now())
            .build()

        event.createFollowup()
            .withEmbeds(embed)
            .withEphemeral(ephemeral)
            .awaitSingleOrNull()
    }

    suspend fun handleIfHasLifecyclePermission(ticket: Ticket, event: DeferrableInteractionEvent, settings: GuildSettings): Boolean {
        return if (ticket.participants.contains(event.interaction.user.id)) {
            // User was added as a participant and can't alter lifecycle of tickets
            event.createFollowup(localeService.getString(settings.locale, "generic.error.lifecycle-permission-denied"))
                .withEphemeral(true)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, genericMessageDeleteSeconds) }
                .awaitSingleOrNull()
            false
        } else true
    }
}
