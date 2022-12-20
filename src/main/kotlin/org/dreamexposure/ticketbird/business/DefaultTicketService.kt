package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.core.spec.EmbedCreateSpec
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.TicketCache
import org.dreamexposure.ticketbird.database.TicketData
import org.dreamexposure.ticketbird.database.TicketRepository
import org.dreamexposure.ticketbird.extensions.embedDescriptionSafe
import org.dreamexposure.ticketbird.`object`.Project
import org.dreamexposure.ticketbird.`object`.Ticket
import org.dreamexposure.ticketbird.utils.GlobalVars
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.getBean
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class DefaultTicketService(
    private val ticketRepository: TicketRepository,
    private val ticketCache: TicketCache,
    private val beanFactory: BeanFactory,
    private val settingsService: GuildSettingsService,
    private val localeService: LocaleService,
    private val permissionService: PermissionService,
    private val componentService: ComponentService,
) : TicketService {

    private val discordClient
        get() = beanFactory.getBean<GatewayDiscordClient>()

    override suspend fun getTicket(guildId: Snowflake, number: Int): Ticket? {
        return getAllTickets(guildId).firstOrNull { it.number == number }
    }

    override suspend fun getTicket(guildId: Snowflake, channelId: Snowflake): Ticket? {
        return getAllTickets(guildId).firstOrNull { it.channel == channelId }
    }

    override suspend fun getAllTickets(guildId: Snowflake): List<Ticket> {
        var tickets = ticketCache.get(guildId.asLong())?.toList()
        if (tickets != null) return tickets

        tickets = ticketRepository.findByGuildId(guildId.asLong())
            .map(::Ticket)
            .collectList()
            .awaitSingle()

        ticketCache.put(guildId.asLong(), tickets.toTypedArray())
        return tickets
    }

    override suspend fun createTicket(ticket: Ticket): Ticket {
        val newTicket = ticketRepository.save(TicketData(
            guildId = ticket.guildId.asLong(),
            number = ticket.number,
            project = ticket.project,
            creator = ticket.creator.asLong(),
            channel = ticket.channel.asLong(),
            category = ticket.category.asLong(),
            lastActivity = ticket.lastActivity.toEpochMilli(),
        )).map(::Ticket).awaitSingle()

        val cached = ticketCache.get(ticket.guildId.asLong())
        if (cached != null) ticketCache.put(ticket.guildId.asLong(), cached + newTicket)

        return newTicket
    }

    override suspend fun updateTicket(ticket: Ticket) {
        ticketRepository.updateByGuildIdAndNumber(
            guildId = ticket.guildId.asLong(),
            number = ticket.number,
            project = ticket.project,
            creator = ticket.creator.asLong(),
            channel = ticket.channel.asLong(),
            category = ticket.category.asLong(),
            lastActivity = ticket.lastActivity.toEpochMilli()
        ).awaitSingleOrNull()

        val cached = ticketCache.get(ticket.guildId.asLong())
        if (cached != null) {
            val newList = cached.toMutableList()
            newList.removeIf { it.number == ticket.number }
            ticketCache.put(ticket.guildId.asLong(), (newList + ticket).toTypedArray())
        }
    }

    override suspend fun deleteTicket(guildId: Snowflake, number: Int) {
        ticketRepository.deleteByGuildIdAndNumber(guildId.asLong(), number).awaitSingleOrNull()

        val cached = ticketCache.get(guildId.asLong())
        if (cached != null) {
            val newList = cached.toMutableList()
            newList.removeIf { it.number == number }
            ticketCache.put(guildId.asLong(), newList.toTypedArray())
        }
    }

    override suspend fun deleteAllTickets(guildId: Snowflake) {
        ticketRepository.deleteAllByGuildId(guildId.asLong()).awaitSingleOrNull()
        ticketCache.evict(guildId.asLong())
    }

    override suspend fun closeTicket(guildId: Snowflake, channelId: Snowflake, inactive: Boolean) {
        val ticket = getTicket(guildId, channelId) ?: return // return if ticket does not exist
        val settings = settingsService.getGuildSettings(guildId)
        val channel = discordClient.getChannelById(channelId).ofType(TextChannel::class.java).awaitSingle()

        channel.edit().withParentIdOrNull(settings.closeCategory)
            .doOnNext { ticket.category = settings.closeCategory!! }
            .doOnNext { ticket.lastActivity = Instant.now() }
            .awaitSingle()
        updateTicket(ticket)

        val field = if (inactive) "ticket.close.inactive" else "ticket.close.generic"


        channel.createMessage(localeService.getString(settings.locale, field, ticket.creator.asString())).awaitSingleOrNull()
    }

    override suspend fun holdTicket(guildId: Snowflake, channelId: Snowflake) {
        val ticket = getTicket(guildId, channelId) ?: return // return if ticket does not exist
        val settings = settingsService.getGuildSettings(guildId)
        val channel = discordClient.getChannelById(channelId).ofType(TextChannel::class.java).awaitSingle()

        channel.edit().withParentIdOrNull(settings.holdCategory)
            .doOnNext { ticket.category = settings.holdCategory!! }
            .doOnNext { ticket.lastActivity = Instant.now() }
            .awaitSingle()
        updateTicket(ticket)

        channel.createMessage(
            localeService.getString(settings.locale, "ticket.hold.creator", ticket.creator.asString())
        ).awaitSingleOrNull()
    }

    override suspend fun purgeTicket(guildId: Snowflake, channelId: Snowflake) {
        val ticket = getTicket(guildId, channelId) ?: return // return if ticket does not exist
        val settings = settingsService.getGuildSettings(guildId)
        val channel = discordClient.getChannelById(channelId).ofType(TextChannel::class.java).awaitSingle()

        channel.delete(localeService.getString(settings.locale, "ticket.delete.time")).awaitSingleOrNull()
        deleteTicket(guildId, ticket.number)
    }

    override suspend fun moveTicket(guildId: Snowflake, channelId: Snowflake, toCategory: Snowflake, withActivity: Boolean) {
        val ticket = getTicket(guildId, channelId) ?: return // return if ticket does not exist
        val channel = discordClient.getChannelById(channelId).ofType(TextChannel::class.java).awaitSingle()

        ticket.category = toCategory
        if (withActivity) ticket.lastActivity = Instant.now()

        updateTicket(ticket)
        channel.edit().withParentIdOrNull(toCategory).awaitSingleOrNull()
    }

    override suspend fun createTicketChannel(guildId: Snowflake, creator: Snowflake, project: Project?, number: Int): TextChannel {
        val settings = settingsService.getGuildSettings(guildId)
        val guild = discordClient.getGuildById(guildId).awaitSingle()

        val name = if (project != null) "${project.prefix}-ticket-$number" else "ticket-$number"

        return guild.createTextChannel(name)
            .withPermissionOverwrites(permissionService.getTicketChannelOverwrites(settings, creator, project))
            .withParentId(settings.awaitingCategory!!)
            .withReason(localeService.getString(settings.locale, "env.channel.ticket.create-reason"))
            .awaitSingle()
    }

    override suspend fun createNewTicketFull(guildId: Snowflake, creatorId: Snowflake, project: Project?, info: String?): Ticket {
        // Get stuff
        val creator = discordClient.getMemberById(guildId, creatorId).awaitSingle()
        val settings = settingsService.getGuildSettings(guildId)
        val ticketNumber = settings.nextId

        // Update settings
        settings.nextId++
        settingsService.updateGuildSettings(settings)

        // Build embed
        val embedBuilder = EmbedCreateSpec.builder()
            .author("@${creator.displayName}", null, creator.avatarUrl)
            .color(GlobalVars.embedColor)
            .timestamp(Instant.now())
        if (!project?.name.isNullOrBlank()) embedBuilder.title(project!!.name)
        if (!info.isNullOrBlank()) embedBuilder.description(info.embedDescriptionSafe())

        // Create ticket channel + message
        val channel = createTicketChannel(guildId, creatorId, project, ticketNumber)

        channel.createMessage()
            .withContent(localeService.getString(settings.locale, "ticket.open.message", creatorId.asString()))
            .withEmbeds(embedBuilder.build())
            .withComponents(*componentService.getTicketMessageComponents(settings))
            .awaitSingle()

        return createTicket(Ticket(
            guildId = guildId,
            number = ticketNumber,
            project = project?.name.orEmpty(),
            creator = creatorId,
            channel = channel.id,
            category = settings.awaitingCategory!!,
            lastActivity = Instant.now()
        ))
    }
}
