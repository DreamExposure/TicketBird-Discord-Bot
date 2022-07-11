package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.PermissionOverwrite
import discord4j.core.`object`.entity.channel.Category
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.getBean
import org.springframework.stereotype.Component

@Component
class DefaultEnvironmentService(
    private val beanFactory: BeanFactory,
    private val settingsService: GuildSettingsService,
    private val permissionService: PermissionService,
    private val localeService: LocaleService,
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
}
