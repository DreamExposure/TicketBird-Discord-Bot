package org.dreamexposure.ticketbird.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.entity.Member
import discord4j.rest.util.Permission
import kotlinx.coroutines.reactor.awaitSingle
import org.dreamexposure.ticketbird.business.*
import org.dreamexposure.ticketbird.extensions.getHumanReadableMinimized
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*

@Component
class SetupCommand(
    private val settingsService: GuildSettingsService,
    private val permissionService: PermissionService,
    private val staticMessageService: StaticMessageService,
    private val componentService: ComponentService,
    private val environmentService: EnvironmentService,
    private val localeService: LocaleService,
) : SlashCommand {
    override val name = "setup"
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
            "init" -> init(event, settings)
            "repair" -> repair(event, settings)
            "language" -> language(event, settings)
            "use-projects" -> useProjects(event, settings)
            "timing" -> timing(event, settings)
            else -> throw IllegalStateException("Invalid subcommand specified")
        }
    }

    private suspend fun init(event: ChatInputInteractionEvent, settings: GuildSettings) {
        // check if setup has already been done
        if (!settings.hasRequiredIdsSet() && !settings.requiresRepair) {
            event.createFollowup(localeService.getString(settings.locale, "command.setup.init.already"))
                .withEphemeral(ephemeral)
                .awaitSingle()
            return
        }

        // check if bot is missing required permissions in order to operate
        val missingPerms = permissionService.checkingMissingBasePermissionsBot(settings.guildId).map(Permission::name)
        if (missingPerms.isNotEmpty()) {
            event.createFollowup(localeService.getString(
                settings.locale,
                "command.setup.bot-missing-perms",
                missingPerms.joinToString("\n")
            )).withEphemeral(ephemeral)
                .awaitSingle()
            return
        }

        // Begin setup

        // Create categories
        settings.awaitingCategory = environmentService.createCategory(settings.guildId, "awaiting").id
        settings.respondedCategory = environmentService.createCategory(settings.guildId, "responded").id
        settings.holdCategory = environmentService.createCategory(settings.guildId, "hold").id
        settings.closeCategory = environmentService.createCategory(settings.guildId, "closed").id

        // Create support channel
        val supportChannel = environmentService.createSupportChannel(settings.guildId)
        settings.supportChannel = supportChannel.id

        // Create static message
        val embed = staticMessageService.getEmbed(settings) ?: throw IllegalStateException("Failed to get embed during setup")
        supportChannel.createMessage(embed)
            .withComponents(*componentService.getStaticMessageComponents(settings))
            .doOnNext { settings.staticMessage = it.id }
            .awaitSingle()

        settingsService.createOrUpdateGuildSettings(settings)

        event.createFollowup(localeService.getString(settings.locale, "command.setup.init.success"))
            .withEphemeral(ephemeral)
            .awaitSingle()
    }

    private suspend fun repair(event: ChatInputInteractionEvent, settings: GuildSettings) {
        // Check if setup has already been done
        if (!settings.requiresRepair && settings.hasRequiredIdsSet()) {
            event.createFollowup(localeService.getString(settings.locale, "command.setup.repair.never-init"))
                .withEphemeral(ephemeral)
                .awaitSingle()
            return
        }

        // Check if bot has correct base permissions in order to set up
        val missingPerms = permissionService.checkingMissingBasePermissionsBot(settings.guildId).map(Permission::name)
        if (missingPerms.isNotEmpty()) {
            event.createFollowup(localeService.getString(
                settings.locale,
                "command.setup.bot-missing-perms",
                missingPerms.joinToString("\n")
            )).withEphemeral(ephemeral)
                .awaitSingle()
            return
        }

        // Validate that all required discord entities exist
        if (environmentService.validateAllEntitiesExist(settings.guildId)) {
            // Everything exists, return
            event.createFollowup(localeService.getString(settings.locale, "command.setup.repair.no-issue-detected"))
                .withEphemeral(ephemeral)
                .awaitSingle()
            return
        }

        environmentService.recreateMissingEntities(settings.guildId)
        staticMessageService.update(settings.guildId)

        //  Respond with success
        event.createFollowup(localeService.getString(settings.locale, "command.setup.repair.success"))
            .withEphemeral(ephemeral)
            .awaitSingle()
    }

    private suspend fun language(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val newLocale = event.options[0].getOption("language")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .map(Locale::forLanguageTag)
            .get()

        settings.locale = newLocale
        settingsService.createOrUpdateGuildSettings(settings)

        event.createFollowup(localeService.getString(settings.locale, "command.setup.language.success"))
            .withEphemeral(ephemeral)
            .awaitSingle()
    }

    private suspend fun useProjects(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val useProjects = event.options[0].getOption("use")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asBoolean)
            .get()

        settings.useProjects = useProjects
        settingsService.createOrUpdateGuildSettings(settings)

        event.createFollowup(localeService.getString(settings.locale, "command.setup.use-projects.success.$useProjects"))
            .withEphemeral(ephemeral)
            .awaitSingle()
    }

    private suspend fun timing(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val action = event.options[0].getOption("action")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .get()
        val days = event.options[0].getOption("days")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asLong)
            .map(Duration::ofDays)
            .orElse(Duration.ZERO)
        val hours = event.options[0].getOption("hours")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asLong)
            .map(Duration::ofHours)
            .orElse(Duration.ZERO)

        // Cannot be zero
        if (days + hours <= Duration.ZERO) {
            event.createFollowup(localeService.getString(settings.locale, "command.setup.timing.error.duration-zero"))
                .withEphemeral(ephemeral)
                .awaitSingle()
            return
        }

        // Apply
        when (action) {
            "auto-close" -> settings.autoClose = days + hours
            "auto-delete" -> settings.autoDelete = days + hours
            else -> {
                event.createFollowup(
                    localeService.getString(settings.locale, "command.setup.timing.error.action-not-found")
                ).withEphemeral(ephemeral).awaitSingle()
                return
            }
        }

        settingsService.createOrUpdateGuildSettings(settings)

        event.createFollowup(localeService.getString(
            settings.locale,
            field = "command.setup.timing.success.$action",
            values = arrayOf((days + hours).getHumanReadableMinimized())
        )).withEphemeral(ephemeral).awaitSingle()
    }
}
