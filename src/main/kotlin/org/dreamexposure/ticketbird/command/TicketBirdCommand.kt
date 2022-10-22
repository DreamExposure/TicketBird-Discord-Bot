package org.dreamexposure.ticketbird.command

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.spec.EmbedCreateSpec
import kotlinx.coroutines.reactor.awaitSingle
import org.dreamexposure.ticketbird.GitProperty.TICKETBIRD_VERSION
import org.dreamexposure.ticketbird.GitProperty.TICKETBIRD_VERSION_D4J
import org.dreamexposure.ticketbird.TicketBird
import org.dreamexposure.ticketbird.business.LocaleService
import org.dreamexposure.ticketbird.extensions.getHumanReadable
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.utils.GlobalVars
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class TicketBirdCommand(
    @Value("\${bot.url.base}")
    private val baseUrl: String,
    @Value("\${bot.url.support}")
    private val supportUrl: String,
    @Value("\${bot.url.invite}")
    private val inviteUrl: String,
    private val localeService: LocaleService,
): SlashCommand {
    override val name = "ticketbird"
    override val ephemeral = false

    override suspend fun handle(event: ChatInputInteractionEvent, settings: GuildSettings) {
        val guilds = event.client.guilds.count().awaitSingle()

        val builder = EmbedCreateSpec.builder()
            .author(localeService.getString(settings.locale, "bot.name"), null, GlobalVars.iconUrl)
            .color(GlobalVars.embedColor)
            .title(localeService.getString(settings.locale, "embed.info.title"))
            .addField(localeService.getString(settings.locale, "embed.info.field.version"), TICKETBIRD_VERSION.value, false)
            .addField(localeService.getString(settings.locale, "embed.info.field.library"), TICKETBIRD_VERSION_D4J.value, false)
            .addField(localeService.getString(settings.locale, "embed.info.field.shard"), formattedIndex(), true)
            .addField(localeService.getString(settings.locale, "embed.info.field.guilds"), "$guilds", true)
            .addField(
                TicketBird.getUptime().getHumanReadable(),
                localeService.getString(settings.locale, "embed.info.field.uptime"),
                false
            ).addField(
                localeService.getString(settings.locale, "embed.info.field.links"),
                localeService.getString(
                    settings.locale,
                    "embed.info.field.links.value",
                    "${baseUrl}/commands",
                    supportUrl,
                    inviteUrl,
                    "https://www.patreon.com/Novafox"
                ),
                false
            ).footer(localeService.getString(settings.locale, "embed.info.footer"), null)
            .timestamp(Instant.now())

        // Even tho this is an info command, we want this state to be easily visible
        if (settings.requiresRepair) {
            builder.addField(
                localeService.getString(settings.locale, "embed.field.warning"),
                localeService.getString(settings.locale, "generic.repair-required"),
                false
            )
        }

        event.createFollowup()
            .withEmbeds(builder.build())
            .withEphemeral(ephemeral)
            .awaitSingle()
    }

    private fun formattedIndex() = "${TicketBird.getShardIndex()}/${TicketBird.getShardCount()}"
}
