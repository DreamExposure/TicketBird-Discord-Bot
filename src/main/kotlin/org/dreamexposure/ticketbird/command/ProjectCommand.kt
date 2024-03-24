package org.dreamexposure.ticketbird.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.spec.EmbedCreateSpec
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.PermissionService
import org.dreamexposure.ticketbird.business.ProjectService
import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.extensions.asSeconds
import org.dreamexposure.ticketbird.extensions.discord4j.deleteFollowupDelayed
import org.dreamexposure.ticketbird.extensions.embedFieldSafe
import org.dreamexposure.ticketbird.extensions.embedTitleSafe
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.`object`.Project
import org.dreamexposure.ticketbird.utils.GlobalVars
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ProjectCommand(
    private val projectService: ProjectService,
    private val permissionService: PermissionService,
    private val localeService: LocaleService,
) : SlashCommand {
    override val name = "project"
    override val hasSubcommands = true
    override val ephemeral = true

    private val messageDeleteSeconds = Config.TIMING_MESSAGE_DELETE_GENERIC_SECONDS.getLong().asSeconds()

    override suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings) {
        // Check permission server-side just in case
        val memberPermissions = event.interaction.member.map(Member::getBasePermissions).get().awaitSingle()
        if (!permissionService.hasRequiredElevatedPermissions(memberPermissions)) {
            event.createFollowup(localeService.getString(settings.locale, "command.project.missing-perms"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        when (event.options[0].name) {
            "add" -> add(event, settings)
            "remove" -> remove(event, settings)
            "list" -> list(event, settings)
            "view" -> view(event, settings)
            "edit" -> edit(event, settings)
            else -> throw IllegalStateException("Invalid subcommand specified")
        }
    }

    private suspend fun add(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val name = event.options[0].getOption("name")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .map { it.substring(0, 100.coerceAtMost(it.length)) }
            .get()
        val prefix = event.options[0].getOption("prefix")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .map { it.replace(Regex("\\W"), "") } // Keep only alphanumeric chars
            .map { it.substring(0, 16.coerceAtMost(it.length)) }
            .get()

        // Check if max amount of projects has been created
        if (projectService.getAllProjects(settings.guildId).size >= 25) {
            event.createFollowup(localeService.getString(settings.locale, "command.project.add.limit-reached"))
                .withEmbeds(listEmbed(settings))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        projectService.createProject(Project(guildId = settings.guildId, name = name, prefix = prefix))

        event.createFollowup(localeService.getString(settings.locale, "command.project.add.success"))
            .withEmbeds(listEmbed(settings))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    private suspend fun remove(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val projectId = try {
            event.options[0].getOption("project")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(String::toLong)
                .get()
        } catch (ex: NumberFormatException) { -1 }


        if (projectService.getProject(settings.guildId, projectId) == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.project.remove.not-found"))
                .withEmbeds(listEmbed(settings))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        projectService.deleteProject(settings.guildId, projectId)

        event.createFollowup(localeService.getString(settings.locale, "command.project.remove.success"))
            .withEmbeds(listEmbed(settings))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    private suspend fun view(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val projectId = try {
            event.options[0].getOption("project")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(String::toLong)
                .get()
        } catch (ex: NumberFormatException) { -1 }

        val project = projectService.getProject(settings.guildId, projectId)
        if (project == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.project.view.not-found"))
                .withEmbeds(listEmbed(settings))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        event.createFollowup()
            .withEmbeds(viewEmbed(settings, project))
            .withEphemeral(ephemeral)
            .awaitSingleOrNull()
    }

    private suspend fun edit(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val projectId = try {
            event.options[0].getOption("project")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(String::toLong)
                .get()
        } catch (ex: NumberFormatException) { -1 }
        val project = projectService.getProject(settings.guildId, projectId)

        val prefix = event.options[0].getOption("prefix")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .map { it.replace(Regex("\\W"), "") } // Keep only alphanumeric chars
            .map { it.substring(0, 16.coerceAtMost(it.length)) }
            .orElse(project?.prefix)
        val pingOverride = event.options[0].getOption("ping-override")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asLong)
            .map(Long::toInt)
            .map(Project.PingOverride::valueOf)
            .orElse(project?.pingOverride)
        val staffUser = event.options[0].getOption("staff-user")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asSnowflake)
            .orElse(null)
        val staffRole = event.options[0].getOption("staff-role")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asSnowflake)
            .orElse(null)
        val removeAllStaff = event.options[0].getOption("remove-all-staff")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asBoolean)
            .orElse(false)

        // Check if project doesn't exist and short circuit.
        if (project == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.project.edit.not-found"))
                .withEmbeds(listEmbed(settings))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        // Modify as needed
        val staffUsers = project.staffUsers.toMutableList()
        val staffRoles = project.staffRoles.toMutableList()

        if (removeAllStaff) {
            staffUsers.clear()
            staffRoles.clear()
        }

        if (staffUser != null) {
            if (staffUsers.contains(staffUser)) staffUsers.remove(staffUser)
            else staffUsers.add(staffUser)
        }
        if (staffRole != null) {
            if (staffRoles.contains(staffRole)) staffRoles.remove(staffRole)
            else staffRoles.add(staffRole)
        }
        projectService.updateProject(project.copy(
            prefix = prefix,
            staffUsers = staffUsers,
            staffRoles = staffRoles,
            pingOverride = pingOverride,
        ))
        val updatedProject = projectService.getProject(settings.guildId, project.id)!!

        // Return with project view embed
        event.createFollowup(localeService.getString(settings.locale, "command.project.edit.success"))
            .withEmbeds(viewEmbed(settings, updatedProject))
            .withEphemeral(ephemeral)
            .awaitSingleOrNull()
    }

    private suspend fun list(event: ChatInputInteractionEvent, settings: GuildSettings) {
        event.createFollowup()
            .withEmbeds(listEmbed(settings))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    private suspend fun listEmbed(settings: GuildSettings): EmbedCreateSpec {
        val projects = projectService.getAllProjects(settings.guildId)

        val builder = EmbedCreateSpec.builder()
            .author(localeService.getString(settings.locale, "bot.name"), null, GlobalVars.iconUrl)
            .color(GlobalVars.embedColor)
            .title(localeService.getString(settings.locale, "embed.projects.title"))
            .timestamp(Instant.now())
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

    private fun viewEmbed(settings: GuildSettings, project: Project): EmbedCreateSpec {
        val builder = EmbedCreateSpec.builder()
            .author(localeService.getString(settings.locale, "bot.name"), null, GlobalVars.iconUrl)
            .color(GlobalVars.embedColor)
            .title(project.name.embedTitleSafe())
            .timestamp(Instant.now())
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
