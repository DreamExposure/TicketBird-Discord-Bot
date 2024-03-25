package org.dreamexposure.ticketbird.commands.global

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.rest.util.AllowedMentions
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.EmbedService
import org.dreamexposure.ticketbird.business.GuildSettingsService
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.business.PermissionService
import org.dreamexposure.ticketbird.commands.SlashCommand
import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.extensions.asSeconds
import org.dreamexposure.ticketbird.extensions.discord4j.deleteFollowupDelayed
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component

@Component
class StaffCommand(
    private val settingsService: GuildSettingsService,
    private val localeService: LocaleService,
    private val permissionService: PermissionService,
    private val embedService: EmbedService,
): SlashCommand {
    override val name = "staff"
    override val hasSubcommands = true
    override val ephemeral = true

    private val messageDeleteSeconds = Config.TIMING_MESSAGE_DELETE_GENERIC_SECONDS.getLong().asSeconds()

    override suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings) {
        // Check permission server-side just in case
        val memberPermissions = event.interaction.member.map(Member::getBasePermissions).get().awaitSingle()
        if (!permissionService.hasRequiredElevatedPermissions(memberPermissions)) {
            event.createFollowup(localeService.getString(settings.locale, "command.setup.missing-perms"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                .awaitSingleOrNull()
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
        settingsService.upsertGuildSettings(settings)

        event.createFollowup(localeService.getString(settings.locale, "command.staff.role.success"))
            .withEmbeds(embedService.getStaffListEmbed(settings))
            .withAllowedMentions(AllowedMentions.suppressAll())
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    private suspend fun add(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val toAddId = event.options[0].getOption("user")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asSnowflake)
            .get()

        if (settings.staff.contains(toAddId.asString())) {
            event.createFollowup(localeService.getString(settings.locale, "command.staff.add.already"))
                .withEmbeds(embedService.getStaffListEmbed(settings))
                .withAllowedMentions(AllowedMentions.suppressAll())
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        settings.staff.add(toAddId.asString())
        settingsService.upsertGuildSettings(settings)

        event.createFollowup(localeService.getString(settings.locale, "command.staff.add.success", toAddId.asString()))
            .withEmbeds(embedService.getStaffListEmbed(settings))
            .withAllowedMentions(AllowedMentions.suppressAll())
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    private suspend fun remove(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val toRemoveId = event.options[0].getOption("user")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asSnowflake)
            .get()

        if (!settings.staff.contains(toRemoveId.asString())) {
            event.createFollowup(localeService.getString(settings.locale, "command.staff.remove.not"))
                .withEmbeds(embedService.getStaffListEmbed(settings))
                .withAllowedMentions(AllowedMentions.suppressAll())
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        settings.staff.remove(toRemoveId.asString())
        settingsService.upsertGuildSettings(settings)

        event.createFollowup(localeService.getString(settings.locale, "command.staff.remove.success", toRemoveId.asString()))
            .withEmbeds(embedService.getStaffListEmbed(settings))
            .withAllowedMentions(AllowedMentions.suppressAll())
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    private suspend fun list(event: ChatInputInteractionEvent, settings: GuildSettings) {
        event.createFollowup()
            .withEmbeds(embedService.getStaffListEmbed(settings))
            .withAllowedMentions(AllowedMentions.suppressAll())
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }
}
