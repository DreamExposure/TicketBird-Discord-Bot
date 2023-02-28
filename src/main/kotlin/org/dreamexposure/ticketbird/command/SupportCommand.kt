package org.dreamexposure.ticketbird.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.entity.Message
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.ComponentService
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.ProjectService
import org.dreamexposure.ticketbird.business.TicketService
import org.dreamexposure.ticketbird.business.cache.CacheRepository
import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.extensions.asSeconds
import org.dreamexposure.ticketbird.extensions.discord4j.deleteFollowupDelayed
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.`object`.TicketCreateState
import org.springframework.stereotype.Component

@Component
class SupportCommand(
    private val projectService: ProjectService,
    private val ticketService: TicketService,
    private val localeService: LocaleService,
    private val componentService: ComponentService,
    private val ticketCreateStateCache: CacheRepository<String, TicketCreateState>,
) : SlashCommand {
    override val name = "support"
    override val ephemeral = true

    private val messageDeleteSeconds = Config.TIMING_MESSAGE_DELETE_TICKET_FLOW_SECONDS.getLong().asSeconds()

    override suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val info = event.getOption("info")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .orElse("")
        val topicId = event.getOption("topic")
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
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
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
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
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
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }
}
