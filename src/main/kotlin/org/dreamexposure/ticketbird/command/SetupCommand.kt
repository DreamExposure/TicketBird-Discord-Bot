package org.dreamexposure.ticketbird.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Permission
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.*
import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.extensions.asSeconds
import org.dreamexposure.ticketbird.extensions.discord4j.deleteFollowupDelayed
import org.dreamexposure.ticketbird.extensions.embedFieldSafe
import org.dreamexposure.ticketbird.extensions.getHumanReadableMinimized
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.utils.GlobalVars
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.*

@Component
class SetupCommand(
    private val settingsService: GuildSettingsService,
    private val projectService: ProjectService,
    private val permissionService: PermissionService,
    private val staticMessageService: StaticMessageService,
    private val componentService: ComponentService,
    private val environmentService: EnvironmentService,
    private val localeService: LocaleService,
) : SlashCommand {
    override val name = "setup"
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
            "init" -> init(event, settings)
            "repair" -> repair(event, settings)
            "language" -> language(event, settings)
            "use-projects" -> useProjects(event, settings)
            "timing" -> timing(event, settings)
            "ping" -> ping(event, settings)
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
            .withEmbeds(viewSettingsEmbed(settings))
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
            .withEmbeds(viewSettingsEmbed(settings))
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

        settings.locale = newLocale
        settingsService.createOrUpdateGuildSettings(settings)

        event.createFollowup(localeService.getString(settings.locale, "command.setup.language.success"))
            .withEmbeds(viewSettingsEmbed(settings))
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

        settings.useProjects = useProjects
        settingsService.createOrUpdateGuildSettings(settings)

        event.createFollowup(localeService.getString(settings.locale, "command.setup.use-projects.success.$useProjects"))
            .withEmbeds(viewSettingsEmbed(settings))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
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
        when (action) {
            "auto-close" -> settings.autoClose = days + hours
            "auto-delete" -> settings.autoDelete = days + hours
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

        settingsService.createOrUpdateGuildSettings(settings)

        event.createFollowup(localeService.getString(
            settings.locale,
            field = "command.setup.timing.success.$action",
            values = arrayOf((days + hours).getHumanReadableMinimized())
        ))
            .withEmbeds(viewSettingsEmbed(settings))
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

        settings.pingOption = pingOption
        settingsService.createOrUpdateGuildSettings(settings)

        event.createFollowup(localeService.getString(
            settings.locale,
            "command.setup.ping.success",
            localeService.getString(settings.locale, pingOption.localeEntry)
        ))
            .withEmbeds(viewSettingsEmbed(settings))
            .withEphemeral(ephemeral)
            .map(Message::getId)
            .flatMap { event.deleteFollowupDelayed(it, messageDeleteSeconds) }
            .awaitSingleOrNull()
    }

    private suspend fun view(event: ChatInputInteractionEvent, settings: GuildSettings) {
        event.createFollowup()
            .withEmbeds(viewSettingsEmbed(settings))
            .withEphemeral(ephemeral)
            .awaitSingleOrNull()
    }

    private suspend fun viewSettingsEmbed(settings: GuildSettings): EmbedCreateSpec {
        val builder = EmbedCreateSpec.builder()
            .author(localeService.getString(settings.locale, "bot.name"), null, GlobalVars.iconUrl)
            .color(GlobalVars.embedColor)
            .title(localeService.getString(settings.locale, "embed.settings.title"))
            .timestamp(Instant.now())
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
}
