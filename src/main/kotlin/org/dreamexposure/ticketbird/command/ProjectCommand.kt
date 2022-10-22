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
import org.dreamexposure.ticketbird.extensions.asSeconds
import org.dreamexposure.ticketbird.extensions.discord4j.deleteFollowupDelayed
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.`object`.Project
import org.dreamexposure.ticketbird.utils.GlobalVars
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ProjectCommand(
    private val projectService: ProjectService,
    private val permissionService: PermissionService,
    private val localeService: LocaleService,
    @Value("\${bot.timing.message-delete.generic.seconds:30}")
    private val messageDeleteSeconds: Long,
): SlashCommand {
    override val name = "project"
    override val ephemeral = true

    override suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings) {
        // Check permission server-side just in case
        val memberPermissions = event.interaction.member.map(Member::getBasePermissions).get().awaitSingle()
        if (!permissionService.hasRequiredElevatedPermissions(memberPermissions)) {
            event.createFollowup(localeService.getString(settings.locale, "command.project.missing-perms"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds.asSeconds()) }
                .awaitSingleOrNull()
            return
        }

        when (event.options[0].name) {
            "add" -> add(event, settings)
            "remove" -> remove(event, settings)
            "list" -> list(event, settings)
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


        // Check if project with same name exists
        if (projectService.getProject(settings.guildId, name) != null) {
            event.createFollowup(localeService.getString(settings.locale, "command.project.add.exists"))
                .withEmbeds(listEmbed(settings))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds.asSeconds()) }
                .awaitSingleOrNull()
            return
        }

        // Check if max amount of projects has been created
        if (projectService.getAllProjects(settings.guildId).size >= 25) {
            event.createFollowup(localeService.getString(settings.locale, "command.project.add.limit-reached"))
                .withEmbeds(listEmbed(settings))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds.asSeconds()) }
                .awaitSingleOrNull()
            return
        }

        projectService.createProject(Project(guildId = settings.guildId, name = name, prefix = prefix))

        event.createFollowup(localeService.getString(settings.locale, "command.project.add.success"))
            .withEmbeds(listEmbed(settings))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds.asSeconds()) }
            .awaitSingleOrNull()
    }

    private suspend fun remove(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val name = event.options[0].getOption("name")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .get()


        if (projectService.getProject(settings.guildId, name) == null) {
            event.createFollowup(localeService.getString(settings.locale, "command.project.remove.not-found"))
                .withEmbeds(listEmbed(settings))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds.asSeconds()) }
                .awaitSingleOrNull()
            return
        }

        projectService.deleteProject(settings.guildId, name)

        event.createFollowup(localeService.getString(settings.locale, "command.project.remove.success"))
            .withEmbeds(listEmbed(settings))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds.asSeconds()) }
            .awaitSingleOrNull()
        return
    }

    private suspend fun list(event: ChatInputInteractionEvent, settings: GuildSettings) {
        event.createFollowup()
            .withEmbeds(listEmbed(settings))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds.asSeconds()) }
            .awaitSingleOrNull()
    }

    private suspend fun listEmbed(settings: GuildSettings): EmbedCreateSpec {
        val projects = projectService.getAllProjects(settings.guildId)

        val builder = EmbedCreateSpec.builder()
            .author(localeService.getString(settings.locale, "bot.name"), null, GlobalVars.iconUrl)
            .color(GlobalVars.embedColor)
            .title(localeService.getString(settings.locale, "embed.projects.title"))
            .timestamp(Instant.now())

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
}
