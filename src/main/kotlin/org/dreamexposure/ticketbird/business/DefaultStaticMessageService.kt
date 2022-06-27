package org.dreamexposure.ticketbird.business

import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.Button
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.Category
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.http.client.ClientException
import kotlinx.coroutines.reactor.awaitSingle
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.utils.GlobalVars.embedColor
import org.dreamexposure.ticketbird.utils.GlobalVars.iconUrl
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class DefaultStaticMessageService(private val localeService: LocaleService) : StaticMessageService {
    override suspend fun getEmbed(guild: Guild, settings: GuildSettings): EmbedCreateSpec? {
        if (settings.awaitingCategory == null || settings.respondedCategory == null || settings.holdCategory == null)
            return null

        val awaiting = guild.getChannelById(settings.awaitingCategory!!).ofType(Category::class.java)
            .flatMap { it.channels.count() }
            .awaitSingle()
        val responded = guild.getChannelById(settings.respondedCategory!!).ofType(Category::class.java)
            .flatMap { it.channels.count() }
            .awaitSingle()
        val hold = guild.getChannelById(settings.holdCategory!!).ofType(Category::class.java)
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

    //TODO: Decide how I actually want to handle this
    override suspend fun update(guild: Guild, settings: GuildSettings): Message? {
        if (settings.supportChannel == null || settings.staticMessage == null) return null

        val button = Button.primary(
            "ticketbird-create-ticket",
            ReactionEmoji.codepoints("TODO"),
            localeService.getString(settings.locale, "button.open-ticket")
        )

        val embed = getEmbed(guild, settings) ?: return null

        guild.getChannelById(settings.supportChannel!!).ofType(TextChannel::class.java)

        guild.getChannelById(settings.supportChannel!!).ofType(TextChannel::class.java).flatMap { channel ->
            channel.getMessageById(settings.staticMessage!!).flatMap { message ->
                // Message exists, just needs the edit
                message.edit().withEmbeds(embed).withComponents(ActionRow.of(button))
            }.onErrorResume(ClientException.isStatusCode(404)) {
                // Message deleted, recreate it
                val message = channel.createMessage(embed).withComponents(ActionRow.of(button)).awaitSingle()

                TODO()
            }
        }


        TODO()
    }
}
