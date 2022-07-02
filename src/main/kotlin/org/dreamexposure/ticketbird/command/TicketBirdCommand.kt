package org.dreamexposure.ticketbird.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.entity.Message
import discord4j.core.spec.EmbedCreateSpec
import kotlinx.coroutines.reactor.awaitSingle
import org.dreamexposure.ticketbird.GitProperty.TICKETBIRD_VERSION
import org.dreamexposure.ticketbird.GitProperty.TICKETBIRD_VERSION_D4J
import org.dreamexposure.ticketbird.TicketBird
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.conf.BotSettings
import org.dreamexposure.ticketbird.extensions.getHumanReadable
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.utils.GlobalVars
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class TicketBirdCommand(
    private val localeService: LocaleService,
): SlashCommand {
    override val name = "ticketbird"
    override val ephemeral = false

    override suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings): Message {
        val guilds = event.client.guilds.count().awaitSingle()

        val embed = EmbedCreateSpec.builder()
            .author(localeService.getString(settings.locale, "bot.name"), null, GlobalVars.iconUrl)
            .color(GlobalVars.embedColor)
            .title(localeService.getString(settings.locale, "embed.info.title"))
            .addField(localeService.getString(settings.locale, "embed.info.field.version"), TICKETBIRD_VERSION.value, false)
            .addField(localeService.getString(settings.locale, "embed.info.field.library"), TICKETBIRD_VERSION_D4J.value, false)
            .addField(localeService.getString(settings.locale, "embed.info.field.shard"), formattedIndex(), true)
            .addField(localeService.getString(settings.locale, "embed.info.field.guilds"), "$guilds", true)
            .addField(
                localeService.getString(settings.locale, "embed.info.field.uptime"),
                TicketBird.getUptime().getHumanReadable(),
                false
            ).addField(
                localeService.getString(settings.locale, "embed.info.field.links"),
                localeService.getString(
                    settings.locale,
                    "embed.info.field.links.value",
                    "${BotSettings.BASE_URL.get()}/commands",
                    BotSettings.SUPPORT_URL.get(),
                    BotSettings.INVITE_URL.get(),
                    "https://www.patreon.com/Novafox"
                ),
                false
            ).footer(localeService.getString(settings.locale, "embed.info.footer"), null)
            .timestamp(Instant.now())
            .build()

        return event.createFollowup()
            .withEmbeds(embed)
            .withEphemeral(ephemeral)
            .awaitSingle()
    }

    private fun formattedIndex() = "${TicketBird.getShardIndex()}/${TicketBird.getShardCount()}"
}
