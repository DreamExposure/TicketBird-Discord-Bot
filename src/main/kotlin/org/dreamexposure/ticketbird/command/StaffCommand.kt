package org.dreamexposure.ticketbird.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.entity.Message
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.AllowedMentions
import kotlinx.coroutines.reactor.awaitSingle
import org.dreamexposure.ticketbird.business.GuildSettingsService
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.utils.GlobalVars
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class StaffCommand(
    private val settingsService: GuildSettingsService,
    private val localeService: LocaleService,
): SlashCommand {
    override val name = "staff"
    override val ephemeral = true

    override suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings): Message {
        return when (event.options[0].name) {
            "add" -> add(event, settings)
            "remove" -> remove(event, settings)
            "list" -> list(event, settings)
            else -> throw IllegalStateException("Invalid subcommand specified")
        }
    }

    private suspend fun add(event: ChatInputInteractionEvent, settings: GuildSettings): Message {
        val toAddId = event.options[0].getOption("user")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asSnowflake)
            .get()

        if (settings.staff.contains(toAddId.asString())) {
            return event.createFollowup(localeService.getString(settings.locale, "command.staff.add.already"))
                .withEphemeral(ephemeral)
                .awaitSingle()
        }

        settings.staff.add(toAddId.asString())
        settingsService.createOrUpdateGuildSettings(settings)

        return event.createFollowup(localeService.getString(settings.locale, "command.staff.add.success", toAddId.asString()))
            .withEphemeral(ephemeral)
            .withAllowedMentions(AllowedMentions.suppressAll())
            .awaitSingle()
    }

    private suspend fun remove(event: ChatInputInteractionEvent, settings: GuildSettings): Message {
        val toRemoveId = event.options[0].getOption("user")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asSnowflake)
            .get()

        if (!settings.staff.contains(toRemoveId.asString())) {
            return event.createFollowup(localeService.getString(settings.locale, "command.staff.remove.not"))
                .withEphemeral(ephemeral)
                .awaitSingle()
        }

        settings.staff.remove(toRemoveId.asString())
        settingsService.createOrUpdateGuildSettings(settings)

        return event.createFollowup(localeService.getString(settings.locale, "command.staff.remove.success", toRemoveId.asString()))
            .withEphemeral(ephemeral)
            .withAllowedMentions(AllowedMentions.suppressAll())
            .awaitSingle()
    }

    private suspend fun list(event: ChatInputInteractionEvent, settings: GuildSettings): Message {
        val staffListStringBuilder = StringBuilder()
        settings.staff.forEach { id -> staffListStringBuilder.append("<@$id>").append("\n") }

        val embed = EmbedCreateSpec.builder()
            .author(localeService.getString(settings.locale, "bot.name"), null, GlobalVars.iconUrl)
            .color(GlobalVars.embedColor)
            .title(localeService.getString(settings.locale, "embed.staff-list.title"))
            .description(staffListStringBuilder.toString())
            .footer(localeService.getString(settings.locale, "embed.staff-list.footer"), null)
            .timestamp(Instant.now())
            .build()

        return event.createFollowup()
            .withEmbeds(embed)
            .withAllowedMentions(AllowedMentions.suppressAll())
            .withEphemeral(ephemeral)
            .awaitSingle()
    }
}
