package org.dreamexposure.ticketbird.business

import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.channel.Category
import discord4j.core.spec.EmbedCreateSpec
import kotlinx.coroutines.reactor.awaitSingle
import org.dreamexposure.ticketbird.GitProperty
import org.dreamexposure.ticketbird.TicketBird
import org.dreamexposure.ticketbird.TicketBird.Companion.getShardCount
import org.dreamexposure.ticketbird.TicketBird.Companion.getShardIndex
import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.extensions.*
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.`object`.Project
import org.dreamexposure.ticketbird.`object`.Ticket
import org.dreamexposure.ticketbird.utils.GlobalVars
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.getBean
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class EmbedService(
    private val projectService: ProjectService,
    private val localeService: LocaleService,
    private val beanFactory: BeanFactory,
) {
    private val discordClient: GatewayDiscordClient
        get() = beanFactory.getBean()

    private suspend fun defaultEmbedBuilder(settings: GuildSettings): EmbedCreateSpec.Builder {
        return EmbedCreateSpec.builder()
            .author(localeService.getString(settings.locale, "bot.name"), null, GlobalVars.iconUrl)
            .color(GlobalVars.embedColor)
            .timestamp(Instant.now())
    }

    ////////////////////////////
    ////// General Embeds //////
    ////////////////////////////
    suspend fun getTicketBirdInfoEmbed(settings: GuildSettings): EmbedCreateSpec {
        val guildCount = discordClient.guilds.count().awaitSingle()

        val builder = defaultEmbedBuilder(settings)
            .title(localeService.getString(settings.locale, "embed.info.title"))
            .addField(localeService.getString(settings.locale, "embed.info.field.version"), GitProperty.TICKETBIRD_VERSION.value, false)
            .addField(localeService.getString(settings.locale, "embed.info.field.library"), "Discord 4J v${GitProperty.TICKETBIRD_VERSION_D4J.value}", false)
            .addField(localeService.getString(settings.locale, "embed.info.field.shard"), "${getShardIndex()}/${getShardCount()}", true)
            .addField(localeService.getString(settings.locale, "embed.info.field.guilds"), "$guildCount", true)
            .addField(
                localeService.getString(settings.locale, "embed.info.field.uptime"),
                TicketBird.getUptime().getHumanReadable(),
                false
            ).addField(
                localeService.getString(settings.locale, "embed.info.field.links"),
                localeService.getString(
                    settings.locale,
                    "embed.info.field.links.value",
                    "${Config.URL_BASE.getString()}/commands",
                    Config.URL_SUPPORT.getString(),
                    Config.URL_INVITE.getString(),
                    "https://www.patreon.com/Novafox"
                ),
                false
            ).footer(localeService.getString(settings.locale, "embed.info.footer"), null)

        // Even tho this is an info command, we want this state to be easily visible
        if (settings.requiresRepair) {
            builder.addField(
                localeService.getString(settings.locale, "embed.field.warning"),
                localeService.getString(settings.locale, "generic.repair-required"),
                false
            )
        }

        return builder.build()
    }

    ///////////////////////////
    ////// Ticket Embeds //////
    ///////////////////////////
    suspend fun getSupportRequestMessageEmbed(settings: GuildSettings): EmbedCreateSpec? {
        val computedTitle = settings.staticMessageTitle ?: localeService.getString(settings.locale, "embed.static.title")
        val computedDescription = settings.staticMessageDescription ?: localeService.getString(settings.locale, "embed.static.desc")

        val builder = defaultEmbedBuilder(settings)
            .title(computedTitle.embedTitleSafe())
            .description(computedDescription.embedDescriptionSafe())
            .footer(localeService.getString(settings.locale, "embed.static.footer"), null)
            .timestamp(Instant.now())

        // Short circuit if repair is required, so we can still make this state visible to end users
        if (settings.requiresRepair || !settings.hasRequiredIdsSet()) {
            builder.addField(
                localeService.getString(settings.locale, "embed.field.warning"),
                localeService.getString(settings.locale, "generic.repair-required"),
                false
            )

            return builder.build()
        }

        // Add ticket stats
        if (settings.showTicketStats) {
            val awaiting = discordClient.getChannelById(settings.awaitingCategory!!).ofType(Category::class.java)
                .flatMap { it.channels.count() }
                .awaitSingle()
            val responded = discordClient.getChannelById(settings.respondedCategory!!).ofType(Category::class.java)
                .flatMap { it.channels.count() }
                .awaitSingle()
            val hold = discordClient.getChannelById(settings.holdCategory!!).ofType(Category::class.java)
                .flatMap { it.channels.count() }
                .awaitSingle()

            val allTickets = settings.nextId - 1
            val closed = allTickets - awaiting - responded - hold
            val open = awaiting + responded

            builder
                .addField(localeService.getString(settings.locale, "embed.static.field.open"), "$open", true)
                .addField(localeService.getString(settings.locale, "embed.static.field.hold"), "$hold", true)
                .addField(localeService.getString(settings.locale, "embed.static.field.closed"), "$closed", true)
        }

        return builder.build()
    }

    suspend fun getChecksumVerificationEmbed(settings: GuildSettings, ticket: Ticket, fileSha: String): EmbedCreateSpec {
       return defaultEmbedBuilder(settings)
            .title(localeService.getString(settings.locale, "embed.checksum.title"))
            .addField(localeService.getString(settings.locale, "embed.checksum.field.checksum"), "`$fileSha`", false)
            .addField(localeService.getString(settings.locale, "embed.checksum.field.ticket"), "ticket-${ticket.number}", false)
            .addField(localeService.getString(settings.locale, "embed.checksum.field.transcript"), "`${ticket.attachmentsSha256 ?: "N/a"}`", true)
            .addField(localeService.getString(settings.locale, "embed.checksum.field.attachments"), "`${ticket.attachmentsSha256 ?: "N/a"}`", true)
            .footer(localeService.getString(settings.locale, "embed.checksum.footer"), null)
            .build()
    }

    suspend fun getTicketOpenEmbed(creator: Member, project: Project?, info: String?): EmbedCreateSpec {
        val embedBuilder = EmbedCreateSpec.builder()
            .author("@${creator.displayName}", null, creator.avatarUrl)
        if (!project?.name.isNullOrBlank()) embedBuilder.title(project!!.name.embedTitleSafe())
        if (!info.isNullOrBlank()) embedBuilder.description(info.embedDescriptionSafe())

        return embedBuilder.build()
    }

    suspend fun getTopicAdditionalInfoEmbed(project: Project, settings: GuildSettings): EmbedCreateSpec {
        return defaultEmbedBuilder(settings)
            .title(localeService.getString(settings.locale, "embed.ticket.additional-info.title"))
            .description(project.additionalInfo?.embedDescriptionSafe() ?: "[Unset, how'd this happen?]")
            .build()
    }

    /////////////////////////////
    ////// Settings Embeds //////
    /////////////////////////////
    suspend fun getViewSettingsEmbed(settings: GuildSettings): EmbedCreateSpec {
        val builder = defaultEmbedBuilder(settings)
            .title(localeService.getString(settings.locale, "embed.settings.title"))
            .footer(localeService.getString(settings.locale, "embed.settings.footer"), null)

        if (settings.hasRequiredIdsSet()) {
            val categories = """
                <#${settings.awaitingCategory?.asString()}>
                <#${settings.respondedCategory?.asString()}>
                <#${settings.holdCategory?.asString()}>
                <#${settings.closeCategory?.asString()}>
            """.trimIndent()
            builder.addField(
                localeService.getString(settings.locale, "embed.settings.field.categories"),
                categories,
                true
            ).addField(
                localeService.getString(settings.locale, "embed.settings.field.support-channel"),
                "<#${settings.supportChannel?.asString()}>",
                true
            )
        } else if (!settings.requiresRepair) {
            // Not init
            builder.addField(
                localeService.getString(settings.locale, "embed.settings.field.categories"),
                localeService.getString(settings.locale, "embed.settings.field.categories.not-init"),
                true
            ).addField(
                localeService.getString(settings.locale, "embed.settings.field.support-channel"),
                localeService.getString(settings.locale, "embed.settings.field.support-channel.not-init"),
                true
            )
        }

        builder.addField(
            localeService.getString(settings.locale, "embed.settings.field.timing"),
            localeService.getString(
                settings.locale,
                "embed.settings.field.timing.value",
                settings.autoClose.getHumanReadableMinimized(),
                settings.autoDelete.getHumanReadableMinimized()
            ),
            false
        ).addField(
            localeService.getString(settings.locale, "embed.settings.field.messaging"),
            localeService.getString(settings.locale, "embed.settings.field.messaging.value"),
            false
        ).addField(
            localeService.getString(settings.locale, "embed.settings.field.language"),
            settings.locale.displayName.embedFieldSafe(),
            true
        ).addField(
            localeService.getString(settings.locale, "embed.settings.field.use-projects"),
            settings.useProjects.toString(),
            true
        ).addField(
            localeService.getString(settings.locale, "embed.settings.field.ping"),
            localeService.getString(settings.locale, settings.pingOption.localeEntry),
            true
        )

        if (settings.enableLogging) {
            builder.addField(
                localeService.getString(settings.locale, "embed.settings.field.logging"),
                localeService.getString(settings.locale, "embed.settings.field.logging.enabled", settings.logChannel?.asString() ?: ""),
                false
            )
        } else {
            builder.addField(
                localeService.getString(settings.locale, "embed.settings.field.logging"),
                localeService.getString(settings.locale, "embed.settings.field.logging.disabled"),
                false
            )
        }
        builder.addField(
            localeService.getString(settings.locale, "embed.settings.field.stats"),
            settings.showTicketStats.toString(),
            true
        )

        // Use projects status notes
        val projects = projectService.getAllProjects(settings.guildId)
        if (!settings.useProjects && projects.isNotEmpty()) {
            builder.addField(
                localeService.getString(settings.locale, "embed.settings.field.note"),
                localeService.getString(settings.locale, "embed.settings.field.note.disabled-with-any"),
                false
            )
        }

        if (settings.useProjects && projects.isEmpty()) {
            builder.addField(
                localeService.getString(settings.locale, "embed.settings.field.note"),
                localeService.getString(settings.locale, "embed.settings.field.note.enabled-and-none"),
                false
            )
        }

        // Warning about needing repair
        if (settings.requiresRepair) {
            builder.addField(
                localeService.getString(settings.locale, "embed.field.warning"),
                localeService.getString(settings.locale, "generic.repair-required"),
                false
            )
        }

        return builder.build()
    }

    suspend fun getStaffListEmbed(settings: GuildSettings): EmbedCreateSpec {
        val staffListStringBuilder = StringBuilder()
        settings.staff.forEach { id -> staffListStringBuilder.append("<@$id>").append("\n") }
        val staffList = staffListStringBuilder.toString()
            .ifBlank { localeService.getString(settings.locale, "embed.staff-list.field.users.none") }

        val staffRole = if (settings.staffRole == null)
            localeService.getString(settings.locale, "embed.staff-list.field.role.none")
        else "<@&${settings.staffRole.asString()}>"

        return defaultEmbedBuilder(settings)
            .title(localeService.getString(settings.locale, "embed.staff-list.title"))
            .addField(localeService.getString(settings.locale, "embed.staff-list.field.users"), staffList, true)
            .addField(localeService.getString(settings.locale, "embed.staff-list.field.role"), staffRole, true)
            .footer(localeService.getString(settings.locale, "embed.staff-list.footer"), null)
            .build()
    }

    ////////////////////////////
    ////// Project Embeds //////
    ////////////////////////////
    suspend fun getProjectListEmbed(settings: GuildSettings): EmbedCreateSpec {
        val projects = projectService.getAllProjects(settings.guildId)

        val builder = defaultEmbedBuilder(settings)
            .title(localeService.getString(settings.locale, "embed.projects.title"))
            .footer(localeService.getString(settings.locale, "embed.projects.footer"), null)

        projects.forEach { project ->
            builder.addField(
                project.name,
                localeService.getString(
                    settings.locale,
                    "embed.projects.field.prefix.value",
                    project.prefix
                ),
                false
            )
        }

        if (!settings.useProjects && projects.isNotEmpty()) {
            builder.addField(
                localeService.getString(settings.locale, "embed.projects.field.note"),
                localeService.getString(settings.locale, "embed.projects.field.note.disabled-with-any"),
                false
            )
        }

        if (settings.useProjects && projects.isEmpty()) {
            builder.addField(
                localeService.getString(settings.locale, "embed.projects.field.note"),
                localeService.getString(settings.locale, "embed.projects.field.note.enabled-and-none"),
                false
            )
        }

        // Even tho this isn't completely related, we still want to make this state visible
        if (settings.requiresRepair) {
            builder.addField(
                localeService.getString(settings.locale, "embed.field.warning"),
                localeService.getString(settings.locale, "generic.repair-required"),
                false
            )
        }

        return builder.build()
    }

    suspend fun getProjectViewEmbed(settings: GuildSettings, project: Project): EmbedCreateSpec {
        val builder = defaultEmbedBuilder(settings)
            .title(project.name.embedTitleSafe())
            .description(project.additionalInfo?.embedDescriptionSafe() ?: localeService.getString(settings.locale, "embed.project-view.description.no-info"))
            .addField(
                localeService.getString(settings.locale, "embed.project-view.field.prefix"),
                project.prefix.embedFieldSafe(),
                true
            ).addField(
                localeService.getString(settings.locale, "embed.project-view.field.example"),
                "${project.prefix}-ticket-${settings.nextId}",
                true,
            ).addField(
                localeService.getString(settings.locale, "embed.project-view.field.ping-override"),
                localeService.getString(settings.locale, project.pingOverride.localeEntry),
                false
            )
            .footer(localeService.getString(settings.locale, "embed.project-view.footer"), null)

        // Staff users
        if (project.staffUsers.isNotEmpty()) {
            val mentions = project.staffUsers.joinToString("\n") { "<@${it.asString()}>" }
            builder.addField(
                localeService.getString(settings.locale, "embed.project-view.field.staff-users"),
                mentions.embedFieldSafe(),
                true
            )
        } else {
            builder.addField(
                localeService.getString(settings.locale, "embed.project-view.field.staff-users"),
                localeService.getString(settings.locale, "embed.project-view.field.staff-users.none"),
                true
            )
        }

        // Staff roles
        if (project.staffRoles.isNotEmpty()) {
            val mentions = project.staffRoles.joinToString("\n") { "<@&${it.asString()}>" }
            builder.addField(
                localeService.getString(settings.locale, "embed.project-view.field.staff-roles"),
                mentions.embedFieldSafe(),
                true
            )
        } else {
            builder.addField(
                localeService.getString(settings.locale, "embed.project-view.field.staff-roles"),
                localeService.getString(settings.locale, "embed.project-view.field.staff-roles.none"),
                true
            )
        }

        // Notes
        if (!settings.useProjects) {
            builder.addField(
                localeService.getString(settings.locale, "embed.project-view.field.note"),
                localeService.getString(settings.locale, "embed.project-view.field.note.disabled-with-any"),
                false
            )
        }

        // Even tho this isn't completely related, we still want to make this state visible
        if (settings.requiresRepair) {
            builder.addField(
                localeService.getString(settings.locale, "embed.field.warning"),
                localeService.getString(settings.locale, "generic.repair-required"),
                false
            )
        }

        return builder.build()
    }
}
