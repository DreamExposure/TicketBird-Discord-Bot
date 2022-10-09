package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.Category
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.http.client.ClientException
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.utils.GlobalVars.embedColor
import org.dreamexposure.ticketbird.utils.GlobalVars.iconUrl
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.getBean
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class DefaultStaticMessageService(
    private val localeService: LocaleService,
    private val settingsService: GuildSettingsService,
    private val componentService: ComponentService,
    private val beanFactory: BeanFactory,
) : StaticMessageService {
    private val discordClient
        get() = beanFactory.getBean<GatewayDiscordClient>()

    override suspend fun getEmbed(settings: GuildSettings): EmbedCreateSpec? {
        val builder = EmbedCreateSpec.builder()
            .author(localeService.getString(settings.locale, "bot.name"), null, iconUrl)
            .color(embedColor)
            .title(localeService.getString(settings.locale, "embed.static.title"))
            .description(localeService.getString(settings.locale, "embed.static.desc"))
            .footer(localeService.getString(settings.locale, "embed.static.footer"), null)
            .timestamp(Instant.now())

        // Short circuit if repair is required, so we can still make this state visible to end users
        if (settings.requiresRepair || !settings.hasRequiredIdsSet()) {
            builder.addField(
                localeService.getString(settings.locale, "embed.field.warning"),
                localeService.getString(settings.locale, "generic.repair-required"),
                false
            )

            return builder.build()
        }

        // Add ticket stats
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

        builder
            .addField(localeService.getString(settings.locale, "embed.static.field.open"), "$open", true)
            .addField(localeService.getString(settings.locale, "embed.static.field.hold"), "$hold", true)
            .addField(localeService.getString(settings.locale, "embed.static.field.closed"), "$closed", true)

        return builder.build()
    }

    override suspend fun update(guildId: Snowflake): Message? {

        val settings = settingsService.getGuildSettings(guildId)

        // Cannot run if support channel or static message not set
        if (settings.supportChannel == null || settings.staticMessage == null) return null

        // Get channel
        val channel = try {
            discordClient.getChannelById(settings.supportChannel!!).ofType(TextChannel::class.java).awaitSingle()
        } catch (ex: ClientException) {
            if (ex.status.code() == 404) settings.staticMessage = null
            if ((ex.status.code() == 403) || (ex.status.code() == 404)) settings.requiresRepair = true

            // Save if needed
            if (settings.requiresRepair) settingsService.updateGuildSettings(settings)

            return null
        }

        // Get message
        val message = try {
            channel.getMessageById(settings.staticMessage!!).awaitSingleOrNull()
        } catch (ex: ClientException) {
            null
        }

        val embed = getEmbed(settings) ?: return null
        return if (message != null) {
            // Update
            message.edit()
                .withContentOrNull("")
                .withEmbeds(embed)
                .withComponents(*componentService.getStaticMessageComponents(settings))
                .awaitSingleOrNull()
        } else {
            // Static message deleted, create new one, update settings
            val newMessage = channel.createMessage(embed)
                .withComponents(*componentService.getStaticMessageComponents(settings))
                .doOnNext { settings.staticMessage = it.id }
                .awaitSingle()

            settingsService.updateGuildSettings(settings)
            newMessage
        }
    }
}
