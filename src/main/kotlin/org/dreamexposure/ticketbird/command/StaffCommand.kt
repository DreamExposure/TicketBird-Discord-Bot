package org.dreamexposure.ticketbird.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Role
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.AllowedMentions
import kotlinx.coroutines.reactor.awaitSingle
import org.dreamexposure.ticketbird.business.GuildSettingsService
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.PermissionService
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.utils.GlobalVars
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class StaffCommand(
    private val settingsService: GuildSettingsService,
    private val localeService: LocaleService,
    private val permissionService: PermissionService,
): SlashCommand {
    override val name = "staff"
    override val ephemeral = true

    override suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings) {
        // Check permission server-side just in case
        val memberPermissions = event.interaction.member.map(Member::getBasePermissions).get().awaitSingle()
        if (!permissionService.hasRequiredElevatedPermissions(memberPermissions)) {
            event.createFollowup(localeService.getString(settings.locale, "command.setup.missing-perms"))
                .withEphemeral(ephemeral)
                .awaitSingle()
            return
        }

        when (event.options[0].name) {
            "role" -> role(event, settings)
            "add" -> add(event, settings)
            "remove" -> remove(event, settings)
            "list" -> list(event, settings)
            else -> throw IllegalStateException("Invalid subcommand specified")
        }
    }

    private suspend fun role(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val roleId = event.options[0].getOption("role")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asSnowflake)
            .orElse(settings.guildId) // everyone role, means to remove staff role

        if (roleId == settings.guildId || roleId.asLong() == 0L) settings.staffRole = null else settings.staffRole = roleId
        settingsService.createOrUpdateGuildSettings(settings)

        event.createFollowup(localeService.getString(settings.locale, "command.staff.role.success"))
            .withEmbeds(getListEmbed(event, settings))
            .withAllowedMentions(AllowedMentions.suppressAll())
            .withEphemeral(ephemeral)
            .awaitSingle()
    }

    private suspend fun add(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val toAddId = event.options[0].getOption("user")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asSnowflake)
            .get()

        if (settings.staff.contains(toAddId.asString())) {
            event.createFollowup(localeService.getString(settings.locale, "command.staff.add.already"))
                .withEmbeds(getListEmbed(event, settings))
                .withAllowedMentions(AllowedMentions.suppressAll())
                .withEphemeral(ephemeral)
                .awaitSingle()
            return
        }

        settings.staff.add(toAddId.asString())
        settingsService.createOrUpdateGuildSettings(settings)

        event.createFollowup(localeService.getString(settings.locale, "command.staff.add.success", toAddId.asString()))
            .withEmbeds(getListEmbed(event, settings))
            .withAllowedMentions(AllowedMentions.suppressAll())
            .withEphemeral(ephemeral)
            .awaitSingle()
    }

    private suspend fun remove(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val toRemoveId = event.options[0].getOption("user")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asSnowflake)
            .get()

        if (!settings.staff.contains(toRemoveId.asString())) {
            event.createFollowup(localeService.getString(settings.locale, "command.staff.remove.not"))
                .withEmbeds(getListEmbed(event, settings))
                .withAllowedMentions(AllowedMentions.suppressAll())
                .withEphemeral(ephemeral)
                .awaitSingle()
            return
        }

        settings.staff.remove(toRemoveId.asString())
        settingsService.createOrUpdateGuildSettings(settings)

        event.createFollowup(localeService.getString(settings.locale, "command.staff.remove.success", toRemoveId.asString()))
            .withEmbeds(getListEmbed(event, settings))
            .withAllowedMentions(AllowedMentions.suppressAll())
            .withEphemeral(ephemeral)
            .awaitSingle()
    }

    private suspend fun list(event: ChatInputInteractionEvent, settings: GuildSettings) {
        event.createFollowup()
            .withEmbeds(getListEmbed(event, settings))
            .withAllowedMentions(AllowedMentions.suppressAll())
            .withEphemeral(ephemeral)
            .awaitSingle()
    }

    private suspend fun getListEmbed(event: ChatInputInteractionEvent, settings: GuildSettings): EmbedCreateSpec {
        val staffListStringBuilder = StringBuilder()
        settings.staff.forEach { id -> staffListStringBuilder.append("<@$id>").append("\n") }
        val staffList = staffListStringBuilder.toString()
            .ifBlank { localeService.getString(settings.locale, "embed.staff-list.field.users.none") }

        val staffRole = if (settings.staffRole == null) {
            localeService.getString(settings.locale, "embed.staff-list.field.role.none")
        } else {
            event.client.getRoleById(settings.guildId, settings.staffRole!!)
                .map(Role::getMention)
                .awaitSingle()
        }

        return EmbedCreateSpec.builder()
            .author(localeService.getString(settings.locale, "bot.name"), null, GlobalVars.iconUrl)
            .color(GlobalVars.embedColor)
            .title(localeService.getString(settings.locale, "embed.staff-list.title"))
            .addField(localeService.getString(settings.locale, "embed.staff-list.field.users"), staffList, true)
            .addField(localeService.getString(settings.locale, "embed.staff-list.field.role"), staffRole, true)
            .footer(localeService.getString(settings.locale, "embed.staff-list.footer"), null)
            .timestamp(Instant.now())
            .build()
    }
}
