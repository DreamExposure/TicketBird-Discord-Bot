package org.dreamexposure.ticketbird.business

import com.fasterxml.jackson.databind.ObjectMapper
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.MessageCreateFields.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import org.dreamexposure.ticketbird.TicketCache
import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.database.TicketData
import org.dreamexposure.ticketbird.database.TicketRepository
import org.dreamexposure.ticketbird.extensions.embedDescriptionSafe
import org.dreamexposure.ticketbird.extensions.sha256Hash
import org.dreamexposure.ticketbird.extensions.ticketLogFileFormat
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.`object`.Project
import org.dreamexposure.ticketbird.`object`.Ticket
import org.dreamexposure.ticketbird.utils.GlobalVars
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.getBean
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.jvm.optionals.getOrNull

@Component
class DefaultTicketService(
    private val ticketRepository: TicketRepository,
    private val ticketCache: TicketCache,
    private val beanFactory: BeanFactory,
    private val settingsService: GuildSettingsService,
    private val localeService: LocaleService,
    private val permissionService: PermissionService,
    private val componentService: ComponentService,
    private val objectMapper: ObjectMapper,
) : TicketService {
    private val discordClient
        get() = beanFactory.getBean<GatewayDiscordClient>()

    init {
        objectMapper.writer()
    }

    override suspend fun getTicket(guildId: Snowflake, number: Int): Ticket? {
        return getAllTickets(guildId).firstOrNull { it.number == number }
    }

    override suspend fun getTicket(guildId: Snowflake, channelId: Snowflake): Ticket? {
        return getAllTickets(guildId).firstOrNull { it.channel == channelId }
    }

    override suspend fun getTicket(guildId: Snowflake, sha256Hash: String): Ticket? {
        return getAllTickets(guildId)
            .firstOrNull { it.transcriptSha256 == sha256Hash || it.attachmentsSha256 == sha256Hash }
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
            transcriptSha256 = ticket.transcriptSha256,
            attachmentsSha256 = ticket.attachmentsSha256,
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
            lastActivity = ticket.lastActivity.toEpochMilli(),
            transcriptSha256 = ticket.transcriptSha256,
            attachmentsSha256 = ticket.attachmentsSha256,
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
        //val ticket = getTicket(guildId, channelId) ?: return // return if ticket does not exist
        val settings = settingsService.getGuildSettings(guildId)
        val channel = discordClient.getChannelById(channelId).ofType(TextChannel::class.java).awaitSingle()

        logTicket(guildId, channelId) // Always make sure to attempt logging before deleting the channel

        channel.delete(localeService.getString(settings.locale, "ticket.delete.time")).awaitSingleOrNull()
        //deleteTicket(guildId, ticket.number)
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

        // Handle ping options
        var calculatedPingOption = when (project?.pingOverride ?: Project.PingOverride.NONE) {
            Project.PingOverride.AUTHOR_ONLY -> GuildSettings.PingOption.AUTHOR_ONLY
            Project.PingOverride.AUTHOR_AND_PROJECT_STAFF -> GuildSettings.PingOption.AUTHOR_AND_PROJECT_STAFF
            Project.PingOverride.AUTHOR_AND_ALL_STAFF -> GuildSettings.PingOption.AUTHOR_AND_ALL_STAFF
            Project.PingOverride.NONE -> settings.pingOption // Default to global setting
        }

        // Change ping setting if project is null or if there's no project staff
        if (calculatedPingOption == GuildSettings.PingOption.AUTHOR_AND_PROJECT_STAFF && (project == null || (project.staffRoles.isEmpty() && project.staffUsers.isEmpty())))
            calculatedPingOption = GuildSettings.PingOption.AUTHOR_ONLY // No project, can't be project

        // Get computed message
        val message = if (calculatedPingOption != GuildSettings.PingOption.AUTHOR_ONLY) {
            val pings = mutableListOf<String>()

            if (calculatedPingOption == GuildSettings.PingOption.AUTHOR_AND_PROJECT_STAFF) {
                pings += project!!.staffUsers.map { "<@${it.asString()}>" }
                pings += project!!.staffRoles.map { "<@&${it.asString()}>" }
            } else if (calculatedPingOption == GuildSettings.PingOption.AUTHOR_AND_ALL_STAFF) {
                pings += settings.staff.map(Snowflake::of).map { "<@${it.asString()}>" }
                if (settings.staffRole != null) pings += "<@&${settings.staffRole!!.asString()}>"

                if (project != null) {
                    pings += project.staffUsers.map { "<@${it.asString()}>" }
                    pings += project.staffRoles.map { "<@&${it.asString()}>" }
                }
            }

            val pingString = pings.joinToString(", ")

            localeService.getString(settings.locale, "ticket.open.message.ping-author-staff", creatorId.asString(), pingString)
        } else {
            localeService.getString(settings.locale, "ticket.open.message.ping-author-only", creatorId.asString())
        }

        // Create message
        channel.createMessage()
            .withContent(message)
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

    override suspend fun logTicket(guildId: Snowflake, channelId: Snowflake) {
        if (!Config.TOGGLE_TICKET_LOGGING.getBoolean()) return

        // Get everything we need
        val settings = settingsService.getGuildSettings(guildId)
        if (!settings.enableLogging || settings.logChannel == null) return // sanity check

        val ticket = getTicket(guildId, channelId) ?: return // return if ticket does not exist
        val ticketChannel = discordClient.getChannelById(channelId).ofType(TextChannel::class.java).awaitSingle()
        val ticketAuthor = discordClient.getUserById(ticket.creator).awaitSingle()
        val finalMessage = ticketChannel.createMessage(localeService.getString(settings.locale, "log.ticket.log-message")).awaitSingle()

        val ticketLog = StringBuilder()
            .appendLine(localeService.getString(settings.locale, "log.ticket.header",
                ticket.number.toString(),
                ticketChannel.name,
                ticketAuthor.username,
                ticketChannel.id.timestamp.ticketLogFileFormat(),
                Instant.now().ticketLogFileFormat())
            ).appendLine()
        val byteStream = ByteArrayOutputStream()
        val zipStream = ZipOutputStream(byteStream)
        var hasAttachments = false


        // Start logging
        ticketChannel.getMessagesAfter(Snowflake.of(0))
            .takeWhile { message ->
                message.id <= finalMessage.id
            }.concatMap { message ->
                mono {
                    val author = message.author.getOrNull()
                    val authorName = author?.username ?: ""
                    val authorId = author?.id?.asString() ?: ""

                    // Start line with formatted info and log content
                    ticketLog.append(localeService.getString(settings.locale, "log.ticket.message",
                        message.timestamp.ticketLogFileFormat(),
                        authorName,
                        authorId,
                        message.content)
                    )

                    // Handle any embeds
                    if (message.embeds.isNotEmpty()) message.embeds.forEach {
                        ticketLog.append(",").append(objectMapper.writeValueAsString(it.data))
                    }

                    // Handle any stickers
                    if (message.stickersItems.isNotEmpty()) message.stickersItems.forEach {
                        ticketLog.append(",").append(objectMapper.writeValueAsString(it.stickerData))
                    }

                    // Handle any attachments
                    if (message.attachments.isNotEmpty()) message.attachments.forEach {
                        ticketLog.append(",").append(objectMapper.writeValueAsString(it.data))
                        // Download attachment to memory and write to zip
                        withContext(Dispatchers.IO) {
                            URL(it.url).openStream().use { attachmentStream ->
                                hasAttachments = true
                                val entry = ZipEntry(it.filename)

                                zipStream.putNextEntry(entry)
                                IOUtils.copy(attachmentStream, zipStream)
                                attachmentStream.close()
                                zipStream.closeEntry()
                            }
                        }
                    }

                    ticketLog.appendLine()
                    message
                }
            }.awaitLast()

        val attachments = mutableListOf<File>()

        // Generate transcript file
        val transcriptStream = ticketLog.toString().byteInputStream()
        ticket.transcriptSha256 = ticketLog.toString().toByteArray().sha256Hash()
        attachments.add(File.of("transcript_ticket-${ticket.number}.log", transcriptStream))
        transcriptStream.close()

        // Generate zipped attachments file
        zipStream.close()
        if (hasAttachments) {
            withContext(Dispatchers.IO) {
                val byteArray = byteStream.toByteArray()
                ticket.attachmentsSha256 = byteArray.sha256Hash()
                val zipInput = ByteArrayInputStream(byteArray)
                attachments.add(File.of("attachments_ticket-${ticket.number}.zip", zipInput))
                zipInput.close()
            }
        }
        byteStream.close()

        updateTicket(ticket)

        discordClient.getChannelById(settings.logChannel!!).ofType(TextChannel::class.java).flatMap { channel ->
            channel.createMessage("").withFiles(attachments)
        }.awaitSingleOrNull()
    }
}
