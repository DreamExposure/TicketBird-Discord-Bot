package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.PermissionOverwrite
import discord4j.core.`object`.entity.Member
import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.getBean
import org.springframework.stereotype.Component

@Component
class DefaultPermissionService(
    private val beanFactory: BeanFactory,
): PermissionService {
    private val discordClient
        get() = beanFactory.getBean<GatewayDiscordClient>()

    override suspend fun checkingMissingBasePermissionsBot(guildId: Snowflake): PermissionSet {
        val current = discordClient.getSelfMember(guildId)
            .flatMap(Member::getBasePermissions)
            .awaitSingle()

        val missing = mutableListOf<Permission>()

        when {
            !current.contains(Permission.ADMINISTRATOR) -> missing.add(Permission.ADMINISTRATOR)
        }

        return PermissionSet.of(*missing.toTypedArray())
    }

    override fun getBotPermissionOverrides() = PermissionSet.of(
        Permission.MANAGE_MESSAGES,
        Permission.SEND_MESSAGES,
        Permission.MANAGE_MESSAGES,
        Permission.EMBED_LINKS,
        Permission.READ_MESSAGE_HISTORY,
        Permission.MENTION_EVERYONE,
        Permission.USE_EXTERNAL_EMOJIS,
        Permission.VIEW_CHANNEL,
    )

    override fun getSupportChannelMemberOverrides() = PermissionSet.of(
        Permission.VIEW_CHANNEL,
        Permission.READ_MESSAGE_HISTORY,
    )

    override fun getTicketGrantOverrides() = PermissionSet.of(
        Permission.MENTION_EVERYONE,
        Permission.ATTACH_FILES,
        Permission.EMBED_LINKS,
        Permission.SEND_MESSAGES,
        Permission.READ_MESSAGE_HISTORY,
        Permission.VIEW_CHANNEL,
        Permission.USE_SLASH_COMMANDS
    )

    override fun getTicketChannelOverwrites(guildId: Snowflake, creator: Snowflake, staff: List<Snowflake>): List<PermissionOverwrite> {
        val overwrites = mutableListOf<PermissionOverwrite>()

        overwrites += PermissionOverwrite.forMember(creator, getTicketGrantOverrides(), PermissionSet.none())
        overwrites += PermissionOverwrite.forRole(guildId, PermissionSet.none(), PermissionSet.all())
        overwrites += staff.map { PermissionOverwrite.forMember(it, getTicketGrantOverrides(), PermissionSet.none()) }

        return overwrites
    }

    override fun hasRequiredElevatedPermissions(memberPermissions: PermissionSet) =
        memberPermissions.contains(Permission.MANAGE_GUILD) || memberPermissions.contains(Permission.ADMINISTRATOR)
}
