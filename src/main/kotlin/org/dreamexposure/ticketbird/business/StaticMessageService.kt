package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.rest.http.client.ClientException
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.logger.LOGGER
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.getBean
import org.springframework.stereotype.Component

@Component
class StaticMessageService(
    private val settingsService: GuildSettingsService,
    private val componentService: ComponentService,
    private val embedService: EmbedService,
    private val beanFactory: BeanFactory,
) {
    private val discordClient
        get() = beanFactory.getBean<GatewayDiscordClient>()

    suspend fun update(guildId: Snowflake): Message? {
        LOGGER.debug("Updating static message | guildId {}", guildId)

        val settings = settingsService.getGuildSettings(guildId)

        // Cannot run if support channel or static message not set
        if (settings.supportChannel == null || settings.staticMessage == null) return null

        // Get channel
        val channel = try {
            discordClient.getChannelById(settings.supportChannel).ofType(TextChannel::class.java).awaitSingle()
        } catch (ex: ClientException) {
            var newSettings = settings

            if (ex.status.code() == 404) newSettings = newSettings.copy(staticMessage = null)
            if ((ex.status.code() == 403) || (ex.status.code() == 404)) newSettings = newSettings.copy(requiresRepair = true)

            // Save if needed
            if (newSettings.requiresRepair) settingsService.updateGuildSettings(newSettings)

            return null
        }

        // Get message
        val message = try {
            channel.getMessageById(settings.staticMessage).awaitSingleOrNull()
        } catch (ex: ClientException) {
            null
        }

        val embed = embedService.getSupportRequestMessageEmbed(settings) ?: return null
        return if (message != null) {
            // Update
            message.edit()
                .withContentOrNull("")
                .withEmbeds(embed)
                .withComponents(*componentService.getStaticMessageComponents(settings))
                .awaitSingleOrNull()
        } else {
            // Static message deleted, create new one, update settings
            var newSettings = settings
            val newMessage = channel.createMessage(embed)
                .withComponents(*componentService.getStaticMessageComponents(settings))
                .doOnNext { newSettings = settings.copy(staticMessage = it.id) }
                .awaitSingle()

            settingsService.updateGuildSettings(newSettings)
            newMessage
        }
    }
}
