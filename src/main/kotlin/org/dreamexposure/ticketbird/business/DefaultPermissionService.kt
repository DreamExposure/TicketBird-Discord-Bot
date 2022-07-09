package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.PermissionOverwrite
import discord4j.core.`object`.entity.Member
import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Component

@Component
class DefaultPermissionService(
    private val discordClient: GatewayDiscordClient,
): PermissionService {
    override suspend fun checkingMissingBasePermissionsBot(guildId: Snowflake): PermissionSet {
        val current = discordClient.getSelfMember(guildId)
            .flatMap(Member::getBasePermissions)
            .awaitSingle()

        val missing = mutableListOf<Permission>()

        when {
            !current.contains(Permission.MANAGE_CHANNELS) -> missing.add(Permission.MANAGE_CHANNELS)
            !current.contains(Permission.SEND_MESSAGES) -> missing.add(Permission.SEND_MESSAGES)
            !current.contains(Permission.MANAGE_MESSAGES) -> missing.add(Permission.MANAGE_MESSAGES)
            !current.contains(Permission.EMBED_LINKS) -> missing.add(Permission.EMBED_LINKS)
            !current.contains(Permission.READ_MESSAGE_HISTORY) -> missing.add(Permission.READ_MESSAGE_HISTORY)
            !current.contains(Permission.MENTION_EVERYONE) -> missing.add(Permission.MENTION_EVERYONE)
            !current.contains(Permission.USE_EXTERNAL_EMOJIS) -> missing.add(Permission.USE_EXTERNAL_EMOJIS)
            !current.contains(Permission.VIEW_CHANNEL) -> missing.add(Permission.VIEW_CHANNEL)
            current.contains(Permission.ADMINISTRATOR) -> missing.clear() // Admin  overrides all of these perms
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

        overwrites += PermissionOverwrite.forMember(guildId, getTicketGrantOverrides(), PermissionSet.none())
        overwrites += PermissionOverwrite.forRole(guildId, PermissionSet.none(), PermissionSet.all())
        overwrites += staff.map { PermissionOverwrite.forMember(it, getTicketGrantOverrides(), PermissionSet.none()) }

        return overwrites
    }

    override fun hasRequiredElevatedPermissions(memberPermissions: PermissionSet) =
        memberPermissions.contains(Permission.MANAGE_GUILD) || memberPermissions.contains(Permission.ADMINISTRATOR)
}
