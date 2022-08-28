package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.PermissionOverwrite
import discord4j.core.`object`.entity.channel.Category
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.rest.http.client.ClientException
import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.getBean
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class DefaultEnvironmentService(
    private val beanFactory: BeanFactory,
    private val settingsService: GuildSettingsService,
    private val permissionService: PermissionService,
    private val localeService: LocaleService,
    private val staticMessageService: StaticMessageService,
    private val componentService: ComponentService,
) : EnvironmentService {
    private val discordClient
        get() = beanFactory.getBean<GatewayDiscordClient>()

    override suspend fun createCategory(guildId: Snowflake, type: String): Category {
        val settings = settingsService.getGuildSettings(guildId)
        val guild = discordClient.getGuildById(guildId).awaitSingle()

        return guild.createCategory(localeService.getString(settings.locale, "env.category.$type.name"))
            .withPermissionOverwrites(PermissionOverwrite.forMember(
                discordClient.selfId,
                permissionService.getBotPermissionOverrides(),
                PermissionSet.none()
            )).awaitSingle()
    }

    override suspend fun createSupportChannel(guildId: Snowflake): TextChannel {
        val settings = settingsService.getGuildSettings(guildId)
        val guild = discordClient.getGuildById(guildId).awaitSingle()

        return guild.createTextChannel(localeService.getString(settings.locale, "env.channel.support.name"))
            .withPermissionOverwrites(
                PermissionOverwrite.forMember(
                    discordClient.selfId,
                    permissionService.getBotPermissionOverrides(),
                    PermissionSet.none()
                ),
                PermissionOverwrite.forRole(
                    guild.id,
                    permissionService.getSupportChannelMemberOverrides(),
                    PermissionSet.of(Permission.SEND_MESSAGES) // Do not allow sending messages in support channel, use interactions
                )
            ).withTopic(localeService.getString(settings.locale, "env.channel.support.topic"))
            .awaitSingle()
    }

    override suspend fun validateAllEntitiesExist(guildId: Snowflake): Boolean {
        val settings = settingsService.getGuildSettings(guildId)
        val guild = discordClient.getGuildById(guildId).awaitSingle()

        var properSetup = settings.hasRequiredIdsSet() && !settings.requiresRepair

        // Check awaiting response ticket category
        if (settings.awaitingCategory != null) {
            guild.getChannelById(settings.awaitingCategory!!)
                .doOnError(ClientException.isStatusCode(404)) { settings.awaitingCategory = null }
                .doOnError(ClientException.isStatusCode(404, 403)) { settings.requiresRepair = true; properSetup = false }
                .onErrorResume { Mono.empty() }
                .awaitSingleOrNull()
        }

        // Check responded ticket category
        if (settings.respondedCategory != null) {
            guild.getChannelById(settings.respondedCategory!!)
                .doOnError(ClientException.isStatusCode(404)) { settings.respondedCategory = null }
                .doOnError(ClientException.isStatusCode(404, 403)) { settings.requiresRepair = true; properSetup = false }
                .onErrorResume { Mono.empty() }
                .awaitSingleOrNull()
        }

        // Check held ticket category
        if (settings.holdCategory != null) {
            guild.getChannelById(settings.holdCategory!!)
                .doOnError(ClientException.isStatusCode(404)) { settings.holdCategory = null }
                .doOnError(ClientException.isStatusCode(404, 403)) { settings.requiresRepair = true; properSetup = false }
                .onErrorResume { Mono.empty() }
                .awaitSingleOrNull()
        }

        // Check closed ticket category
        if (settings.closeCategory != null) {
            guild.getChannelById(settings.closeCategory!!)
                .doOnError(ClientException.isStatusCode(404)) { settings.closeCategory = null }
                .doOnError(ClientException.isStatusCode(404, 403)) { settings.requiresRepair = true; properSetup = false }
                .onErrorResume { Mono.empty() }
                .awaitSingleOrNull()
        }

        // Check support channel
        if (settings.supportChannel != null) {
            guild.getChannelById(settings.supportChannel!!)
                .doOnError(ClientException.isStatusCode(404)) { settings.supportChannel = null }
                .doOnError(ClientException.isStatusCode(404, 403)) { settings.requiresRepair = true; properSetup = false }
                .onErrorResume { Mono.empty() }
                .awaitSingleOrNull()
        }

        // Check static message
        if (settings.supportChannel != null && settings.staticMessage != null) {
            guild.getChannelById(settings.supportChannel!!)
                .ofType(TextChannel::class.java)
                .flatMap { it.getMessageById(settings.staticMessage) }
                .doOnError(ClientException.isStatusCode(404)) { settings.staticMessage = null }
                .doOnError(ClientException.isStatusCode(404, 403)) { settings.requiresRepair = true; properSetup = false }
                .onErrorResume { Mono.empty() }
                .awaitSingleOrNull()
        }

        settingsService.createOrUpdateGuildSettings(settings)
        return properSetup
    }

    override suspend fun recreateMissingEntities(guildId: Snowflake) {
        val settings = settingsService.getGuildSettings(guildId)

        if (settings.awaitingCategory == null) settings.awaitingCategory = createCategory(guildId, "awaiting").id
        if (settings.respondedCategory == null) settings.respondedCategory = createCategory(guildId, "responded").id
        if (settings.holdCategory == null) settings.holdCategory = createCategory(guildId, "hold").id
        if (settings.closeCategory == null) settings.closeCategory = createCategory(guildId, "close").id

        if (settings.supportChannel == null) {
            val supportChannel = createSupportChannel(guildId)
            settings.supportChannel = supportChannel.id

            // Create new static message since the old one is lost now
            val embed = staticMessageService.getEmbed(settings) ?: throw IllegalStateException("Failed to get embed during recreate")
            supportChannel.createMessage(embed)
                .withComponents(*componentService.getStaticMessageComponents(settings))
                .doOnNext { settings.staticMessage = it.id }
                .awaitSingle()
        }

        // Just create new static message
        if (settings.staticMessage == null) {
            // Support channel exists, but not static message, just recreate
            val supportChannel = discordClient.getChannelById(settings.supportChannel!!)
                .ofType(TextChannel::class.java)
                .awaitSingle()

            val embed = staticMessageService.getEmbed(settings) ?: throw IllegalStateException("Failed to get embed during recreate")
            supportChannel.createMessage(embed)
                .withComponents(*componentService.getStaticMessageComponents(settings))
                .doOnNext { settings.staticMessage = it.id }
                .awaitSingle()
        }

        if (settings.hasRequiredIdsSet()) settings.requiresRepair = false
        settingsService.createOrUpdateGuildSettings(settings)
    }
}
