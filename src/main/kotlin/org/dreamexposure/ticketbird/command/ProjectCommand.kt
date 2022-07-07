package org.dreamexposure.ticketbird.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.spec.EmbedCreateSpec
import kotlinx.coroutines.reactor.awaitSingle
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.PermissionService
import org.dreamexposure.ticketbird.business.ProjectService
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
): SlashCommand {
    override val name = "project"
    override val ephemeral = true

    override suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings): Message {
        // Check permission server-side just in case
        val memberPermissions = event.interaction.member.map(Member::getBasePermissions).get().awaitSingle()
        if (!permissionService.hasRequiredElevatedPermissions(memberPermissions)) {
            return event.createFollowup(localeService.getString(settings.locale, "command.project.missing-perms"))
                .withEphemeral(ephemeral)
                .awaitSingle()
        }

        return when (event.options[0].name) {
            "add" -> add(event, settings)
            "remove" -> remove(event, settings)
            "list" -> list(event, settings)
            else -> throw IllegalStateException("Invalid subcommand specified")
        }
    }

    private suspend fun add(event: ChatInputInteractionEvent, settings: GuildSettings): Message {
        val name = event.options[0].getOption("name")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .get()
        val prefix = event.options[0].getOption("prefix")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .map { it.replace(Regex("\\W"), "") } // Keep only alphanumeric chars
            .map { it.substring(0, 16.coerceAtMost(it.length)) }
            .get()


        if (projectService.getProject(settings.guildId, name) != null) {
            return event.createFollowup(localeService.getString(settings.locale, "command.project.add.exists"))
                .withEmbeds(listEmbed(settings))
                .withEphemeral(ephemeral)
                .awaitSingle()
        }

        projectService.createProject(Project(settings.guildId, name = name, prefix = prefix))

        return event.createFollowup(localeService.getString(settings.locale, "command.project.add.success"))
            .withEmbeds(listEmbed(settings))
            .withEphemeral(ephemeral)
            .awaitSingle()
    }

    private suspend fun remove(event: ChatInputInteractionEvent, settings: GuildSettings): Message {
        val name = event.options[0].getOption("name")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .get()


        if (projectService.getProject(settings.guildId, name) == null) {
            return event.createFollowup(localeService.getString(settings.locale, "command.project.remove.not-found"))
                .withEmbeds(listEmbed(settings))
                .withEphemeral(ephemeral)
                .awaitSingle()
        }

        projectService.deleteProject(settings.guildId, name)

        return event.createFollowup(localeService.getString(settings.locale, "command.project.remove.success"))
            .withEmbeds(listEmbed(settings))
            .withEphemeral(ephemeral)
            .awaitSingle()
    }

    private suspend fun list(event: ChatInputInteractionEvent, settings: GuildSettings): Message {
        return event.createFollowup()
            .withEmbeds(listEmbed(settings))
            .withEphemeral(ephemeral)
            .awaitSingle()
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

        return builder.build()
    }
}