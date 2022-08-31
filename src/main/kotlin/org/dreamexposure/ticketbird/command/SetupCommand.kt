package org.dreamexposure.ticketbird.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.rest.util.Permission
import kotlinx.coroutines.reactor.awaitSingle
import org.dreamexposure.ticketbird.business.*
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.springframework.stereotype.Component
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

    override suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings): Message {
        // Check permission server-side just in case
        val memberPermissions = event.interaction.member.map(Member::getBasePermissions).get().awaitSingle()
        if (!permissionService.hasRequiredElevatedPermissions(memberPermissions)) {
            return event.createFollowup(localeService.getString(settings.locale, "command.setup.missing-perms"))
                .withEphemeral(ephemeral)
                .awaitSingle()
        }

        return when (event.options[0].name) {
            "init" -> init(event, settings)
            "repair" -> repair(event, settings)
            "language" -> language(event, settings)
            "use-projects" -> useProjects(event, settings)
            "timing" -> timing(event, settings)
            else -> throw IllegalStateException("Invalid subcommand specified")
        }
    }

    private suspend fun init(event: ChatInputInteractionEvent, settings: GuildSettings): Message {
        // check if setup has already been done
        if (settings.supportChannel != null) {
            return event.createFollowup(localeService.getString(settings.locale, "command.setup.init.already"))
                .withEphemeral(ephemeral)
                .awaitSingle()
        }

        // check if bot is missing required permissions in order to operate
        val missingPerms = permissionService.checkingMissingBasePermissionsBot(settings.guildId).map(Permission::name)
        if (missingPerms.isNotEmpty()) {
            return event.createFollowup(localeService.getString(
                settings.locale,
                "command.setup.bot-missing-perms",
                missingPerms.joinToString("\n")
            )).withEphemeral(ephemeral)
                .awaitSingle()
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

        return event.createFollowup(localeService.getString(settings.locale, "command.setup.init.success"))
            .withEphemeral(ephemeral)
            .awaitSingle()
    }

    private suspend fun repair(event: ChatInputInteractionEvent, settings: GuildSettings): Message {
        // Check if setup has already been done
        if (!settings.requiresRepair && settings.hasRequiredIdsSet()) {
            return event.createFollowup(localeService.getString(settings.locale, "command.setup.repair.never-init"))
                .withEphemeral(ephemeral)
                .awaitSingle()
        }

        // Check if bot has correct base permissions in order to set up
        val missingPerms = permissionService.checkingMissingBasePermissionsBot(settings.guildId).map(Permission::name)
        if (missingPerms.isNotEmpty()) {
            return event.createFollowup(localeService.getString(
                settings.locale,
                "command.setup.bot-missing-perms",
                missingPerms.joinToString("\n")
            )).withEphemeral(ephemeral)
                .awaitSingle()
        }

        // Validate that all required discord entities exist
        if (environmentService.validateAllEntitiesExist(settings.guildId)) {
            // Everything exists, return
            return event.createFollowup(localeService.getString(settings.locale, "command.setup.repair.no-issue-detected"))
                .withEphemeral(ephemeral)
                .awaitSingle()
        }

        environmentService.recreateMissingEntities(settings.guildId)
        staticMessageService.update(settings.guildId)

        //  Respond with success
        return event.createFollowup(localeService.getString(settings.locale, "command.setup.repair.success"))
            .withEphemeral(ephemeral)
            .awaitSingle()
    }

    private suspend fun language(event: ChatInputInteractionEvent, settings: GuildSettings): Message {
        val newLocale = event.options[0].getOption("language")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .map(Locale::forLanguageTag)
            .get()

        settings.locale = newLocale
        settingsService.createOrUpdateGuildSettings(settings)

        return event.createFollowup(localeService.getString(settings.locale, "command.setup.language.success"))
            .withEphemeral(ephemeral)
            .awaitSingle()
    }

    private suspend fun useProjects(event: ChatInputInteractionEvent, settings: GuildSettings): Message {
        val useProjects = event.options[0].getOption("use")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asBoolean)
            .get()

        settings.useProjects = useProjects
        settingsService.createOrUpdateGuildSettings(settings)

        return event.createFollowup(localeService.getString(settings.locale, "command.setup.use-projects.success.$useProjects"))
            .withEphemeral(ephemeral)
            .awaitSingle()
    }

    private suspend fun timing(event: ChatInputInteractionEvent, settings: GuildSettings): Message {
        TODO("Not yet implemented")
    }
}
