package org.dreamexposure.ticketbird.commands.global

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.rest.util.Permission
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.*
import org.dreamexposure.ticketbird.commands.SlashCommand
import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.extensions.asSeconds
import org.dreamexposure.ticketbird.extensions.discord4j.deleteFollowupDelayed
import org.dreamexposure.ticketbird.extensions.getHumanReadableMinimized
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Component
class SetupCommand(
    private val settingsService: GuildSettingsService,
    private val permissionService: PermissionService,
    private val staticMessageService: StaticMessageService,
    private val componentService: ComponentService,
    private val environmentService: EnvironmentService,
    private val embedService: EmbedService,
    private val localeService: LocaleService,
) : SlashCommand {
    override val name = "setup"
    override val hasSubcommands = true
    override val ephemeral = true

    private val messageDeleteSeconds = Config.TIMING_MESSAGE_DELETE_GENERIC_SECONDS.getLong().asSeconds()
    private val doNotDeferOnSubcommands = setOf("messaging")


    override suspend fun shouldDefer(event: ChatInputInteractionEvent) = !doNotDeferOnSubcommands.contains(event.options[0].name)

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
            "init" -> init(event, settings)
            "repair" -> repair(event, settings)
            "language" -> language(event, settings)
            "use-projects" -> useProjects(event, settings)
            "show-ticket-stats" -> showTicketStats(event, settings)
            "messaging" -> editMessaging(event, settings)
            "timing" -> timing(event, settings)
            "ping" -> ping(event, settings)
            "logging" -> logging(event, settings)
            "view" -> view(event, settings)
            else -> throw IllegalStateException("Invalid subcommand specified")
        }
    }

    private suspend fun init(event: ChatInputInteractionEvent, settings: GuildSettings) {
        // check if setup has already been done
        if (settings.hasRequiredIdsSet() && !settings.requiresRepair) {
            event.createFollowup(localeService.getString(settings.locale, "command.setup.init.already"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        // need repair?
        if (settings.requiresRepair) {
            event.createFollowup(localeService.getString(settings.locale, "generic.repair-required"))
                .withEphemeral(ephemeral)
                .awaitSingleOrNull()
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
        var newSettings = settings
        newSettings = newSettings.copy(awaitingCategory = environmentService.createCategory(newSettings.guildId, "awaiting").id)
        newSettings = newSettings.copy(respondedCategory = environmentService.createCategory(newSettings.guildId, "responded").id)
        newSettings = newSettings.copy(holdCategory = environmentService.createCategory(newSettings.guildId, "hold").id)
        newSettings = newSettings.copy(closeCategory = environmentService.createCategory(newSettings.guildId, "closed").id)

        // Create support channel
        val supportChannel = environmentService.createSupportChannel(newSettings.guildId)
        newSettings = newSettings.copy(supportChannel = supportChannel.id)

        // Create static message
        val embed = embedService.getSupportRequestMessageEmbed(newSettings)
            ?: throw IllegalStateException("Failed to get embed during setup")
        supportChannel.createMessage(embed)
            .withComponents(*componentService.getStaticMessageComponents(newSettings))
            .doOnNext { newSettings = newSettings.copy(staticMessage = it.id) }
            .awaitSingle()

        settingsService.upsertGuildSettings(newSettings)

        event.createFollowup(localeService.getString(newSettings.locale, "command.setup.init.success"))
            .withEmbeds(embedService.getViewSettingsEmbed(newSettings))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    private suspend fun repair(event: ChatInputInteractionEvent, settings: GuildSettings) {
        // Check if setup has never been done
        if (!settings.requiresRepair && settings.hasRequiredIdsSet()) {
            event.createFollowup(localeService.getString(settings.locale, "command.setup.repair.never-init"))
                .withEphemeral(ephemeral)
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                .awaitSingleOrNull()
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
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        environmentService.recreateMissingEntities(settings.guildId)
        staticMessageService.update(settings.guildId)

        //  Respond with success
        event.createFollowup(localeService.getString(settings.locale, "command.setup.repair.success"))
            .withEmbeds(embedService.getViewSettingsEmbed(settings))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    private suspend fun language(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val newLocale = event.options[0].getOption("language")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .map(Locale::forLanguageTag)
            .get()

        val newSettings = settings.copy(locale = newLocale)
        settingsService.upsertGuildSettings(newSettings)

        event.createFollowup(localeService.getString(newSettings.locale, "command.setup.language.success"))
            .withEmbeds(embedService.getViewSettingsEmbed(newSettings))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    private suspend fun useProjects(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val useProjects = event.options[0].getOption("use")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asBoolean)
            .get()

        val newSettings = settings.copy(useProjects = useProjects)
        settingsService.upsertGuildSettings(newSettings)

        event.createFollowup(localeService.getString(newSettings.locale, "command.setup.use-projects.success.$useProjects"))
            .withEmbeds(embedService.getViewSettingsEmbed(newSettings))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    private suspend fun showTicketStats(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val showStats = event.options[0].getOption("show")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asBoolean)
            .get()

        val newSettings = settings.copy(showTicketStats = showStats)
        settingsService.upsertGuildSettings(newSettings)
        staticMessageService.update(newSettings.guildId)

        event.createFollowup(localeService.getString(newSettings.locale, "command.setup.show-ticket-stats.success.$showStats"))
            .withEmbeds(embedService.getViewSettingsEmbed(newSettings))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    private suspend fun editMessaging(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val message = event.options[0].getOption("message")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .get()

        when (message.lowercase()) {
            "support-message" -> {
                event.presentModal()
                    .withCustomId("edit-support-message-modal")
                    .withTitle(localeService.getString(settings.locale, "modal.edit-support-message.title"))
                    .withComponents(*componentService.getEditSupportMessageModalComponents(settings))
                    .awaitSingleOrNull()
            } else -> {
                event.reply(
                    localeService.getString(settings.locale, "command.setup.messaging.error.message-type-not-found")
                ).withEphemeral(ephemeral).awaitSingleOrNull()
            }
        }
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
                .map(Message::getId)
                .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                .awaitSingleOrNull()
            return
        }

        // Apply
        val newSettings = when (action) {
            "auto-close" -> settings.copy(autoClose = days + hours)
            "auto-delete" -> settings.copy(autoDelete = days + hours)
            else -> {
                event.createFollowup(
                    localeService.getString(settings.locale, "command.setup.timing.error.action-not-found")
                ).withEphemeral(ephemeral)
                    .map(Message::getId)
                    .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                    .awaitSingleOrNull()
                return
            }
        }

        settingsService.upsertGuildSettings(newSettings)

        event.createFollowup(localeService.getString(
            newSettings.locale,
            field = "command.setup.timing.success.$action",
            values = arrayOf((days + hours).getHumanReadableMinimized())
        ))
            .withEmbeds(embedService.getViewSettingsEmbed(newSettings))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    private suspend fun ping(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val pingOption = event.options[0].getOption("setting")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asLong)
            .map(Long::toInt)
            .map(GuildSettings.PingOption::valueOf)
            .get()

        val newSettings = settings.copy(pingOption = pingOption)
        settingsService.upsertGuildSettings(newSettings)

        event.createFollowup(localeService.getString(
            newSettings.locale,
            "command.setup.ping.success",
            localeService.getString(newSettings.locale, pingOption.localeEntry)
        ))
            .withEmbeds(embedService.getViewSettingsEmbed(newSettings))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    private suspend fun logging(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val loggingEnabled = event.options[0].getOption("enable")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asBoolean)
            .orElse(settings.enableLogging)
        val logChannel = event.options[0].getOption("channel")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asSnowflake)
            .getOrNull() ?: settings.logChannel

        // Validation
        if (loggingEnabled) {
            // Cannot enable logging without a channel to log to
            if (logChannel == null) {
                event.createFollowup(localeService.getString(settings.locale, "command.setup.logging.error.no-channel"))
                    .withEphemeral(ephemeral)
                    .map(Message::getId)
                    .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                    .awaitSingleOrNull()
                return
            }

            // Check perms on channel to make sure TicketBird can actually use it for logging
            if (!environmentService.validateChannelForLogging(settings.guildId, logChannel)) {
                event.createFollowup(localeService.getString(settings.locale, "command.setup.logging.error.channel-invalid"))
                    .withEphemeral(ephemeral)
                    .map(Message::getId)
                    .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
                    .awaitSingleOrNull()
                return
            }
        }

        // Actually update the settings
        val newSettings = settings.copy(enableLogging = loggingEnabled, logChannel = logChannel)
        settingsService.upsertGuildSettings(newSettings)

        val localeString =
            if (Config.TOGGLE_TICKET_LOGGING.getBoolean()) "command.setup.logging.success.with-toggle"
            else "command.setup.logging.success"
        event.createFollowup(localeService.getString(
            newSettings.locale,
            localeString,
            "$loggingEnabled"
        ))
            .withEmbeds(embedService.getViewSettingsEmbed(newSettings))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    private suspend fun view(event: ChatInputInteractionEvent, settings: GuildSettings) {
        event.createFollowup()
            .withEmbeds(embedService.getViewSettingsEmbed(settings))
            .withEphemeral(ephemeral)
            .awaitSingleOrNull()
    }
}
