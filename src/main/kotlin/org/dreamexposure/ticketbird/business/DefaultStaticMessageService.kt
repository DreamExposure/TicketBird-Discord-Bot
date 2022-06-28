package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.Button
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.Category
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.http.client.ClientException
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.utils.GlobalVars.embedColor
import org.dreamexposure.ticketbird.utils.GlobalVars.iconUrl
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class DefaultStaticMessageService(
    private val localeService: LocaleService,
    private val settingsService: GuildSettingsService,
    private val discordClient: GatewayDiscordClient,
) : StaticMessageService {
    override suspend fun getEmbed(guildId: Snowflake): EmbedCreateSpec? {
        val settings = settingsService.getGuildSettings(guildId)

        if (settings.awaitingCategory == null || settings.respondedCategory == null || settings.holdCategory == null)
            return null

        val awaiting = discordClient.getChannelById(settings.awaitingCategory!!).ofType(Category::class.java)
            .flatMap { it.channels.count() }
            .awaitSingle()
        val responded = discordClient.getChannelById(settings.respondedCategory!!).ofType(Category::class.java)
            .flatMap { it.channels.count() }
            .awaitSingle()
        val hold = discordClient.getChannelById(settings.holdCategory!!).ofType(Category::class.java)
            .flatMap { it.channels.count() }
            .awaitSingle()

        val allTickets = settings.nextId - 1
        val closed = allTickets - awaiting - responded - hold
        val open = awaiting + responded

        return EmbedCreateSpec.builder()
            .author(localeService.getString(settings.locale, "bot.name"), null, iconUrl)
            .color(embedColor)
            .title(localeService.getString(settings.locale, "embed.static.title"))
            .description(localeService.getString(settings.locale, "embed.static.desc"))
            .addField(localeService.getString(settings.locale, "embed.static.field.open"), "$open", true)
            .addField(localeService.getString(settings.locale, "embed.static.field.hold"), "$hold", true)
            .addField(localeService.getString(settings.locale, "embed.static.field.closed"), "$closed", true)
            .footer(localeService.getString(settings.locale, "embed.static.footer"), null)
            .timestamp(Instant.now())
            .build()
    }

    override suspend fun update(guildId: Snowflake): Message? {
        val settings = settingsService.getGuildSettings(guildId)

        // Cannot run if support channel or static message not set
        if (settings.supportChannel == null || settings.staticMessage == null) return null

        val button = Button.primary(
            "ticketbird-create-ticket",
            ReactionEmoji.codepoints("TODO"),
            localeService.getString(settings.locale, "button.open-ticket")
        )

        // Get channel
        val channel = try {
            discordClient.getChannelById(settings.supportChannel!!).ofType(TextChannel::class.java).awaitSingle()
        } catch (ex: ClientException) {
            if (ex.status.code() == 403 || ex.status.code() == 404) {
                // Permission denied or channel not found
                settings.staticMessage = null
                settingsService.updateGuildSettings(settings)
            }
            return null
        }

        // Get message
        val message = try {
            channel.getMessageById(settings.staticMessage!!).awaitSingleOrNull()
        } catch (ex: ClientException) {
            null
        }

        val embed = getEmbed(guildId) ?: return null
        return if (message != null) {
            // Update
            message.edit()
                .withEmbeds(embed)
                .withComponents(ActionRow.of(button))
                .awaitSingleOrNull()
        } else {
            // Static message deleted, create new one, update settings
            val newMessage = channel.createMessage(embed)
                .withComponents(ActionRow.of(button))
                .doOnNext { settings.staticMessage = it.id }
                .awaitSingle()

            settingsService.updateGuildSettings(settings)
            newMessage
        }
    }
}
