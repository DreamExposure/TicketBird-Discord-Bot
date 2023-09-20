package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.PermissionOverwrite
import discord4j.core.`object`.entity.Member
import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet
import kotlinx.coroutines.reactor.awaitSingle
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.`object`.Project
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.getBean
import org.springframework.stereotype.Component

@Component
class DefaultPermissionService(
    private val beanFactory: BeanFactory,
) : PermissionService {
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

    override fun getTicketGrantOverrides(write: Boolean): PermissionSet {
        val defaultPerms = PermissionSet.of(
            Permission.VIEW_CHANNEL,
            Permission.READ_MESSAGE_HISTORY,
        )

        return if (write) defaultPerms.or(PermissionSet.of(
            Permission.ATTACH_FILES,
            Permission.EMBED_LINKS,
            Permission.ADD_REACTIONS,
            Permission.SEND_MESSAGES,
            Permission.USE_APPLICATION_COMMANDS,
        )) else defaultPerms
    }

    override fun getTicketChannelOverwrites(settings: GuildSettings, creator: Snowflake, project: Project?): List<PermissionOverwrite> {
        val overwrites = mutableListOf<PermissionOverwrite>()

        overwrites += PermissionOverwrite.forMember(creator, getTicketGrantOverrides(true), PermissionSet.none())
        overwrites += PermissionOverwrite.forRole(settings.guildId, PermissionSet.none(), PermissionSet.all())
        overwrites += settings.staff
            .map(Snowflake::of)
            .map { PermissionOverwrite.forMember(it, getTicketGrantOverrides(true), PermissionSet.none()) }

        if (project != null) {
            overwrites += project.staffUsers.map { PermissionOverwrite.forMember(it, getTicketGrantOverrides(true), PermissionSet.none()) }
            overwrites += project.staffRoles.map { PermissionOverwrite.forRole(it, getTicketGrantOverrides(true), PermissionSet.none()) }
        }
        if (settings.staffRole != null)
            overwrites += PermissionOverwrite.forRole(settings.staffRole!!, getTicketGrantOverrides(true), PermissionSet.none())

        return overwrites
    }

    override fun getTicketParticipantGrantOverwrites(write: Boolean, user: Snowflake): PermissionOverwrite {
        return PermissionOverwrite.forMember(user, getTicketGrantOverrides(write), PermissionSet.none())
    }

    override fun getTicketParticipantDenyOverwrites(user: Snowflake): PermissionOverwrite {
        return PermissionOverwrite.forMember(user, PermissionSet.none(), PermissionSet.all())
    }

    override fun hasRequiredElevatedPermissions(memberPermissions: PermissionSet) =
        memberPermissions.contains(Permission.MANAGE_GUILD) || memberPermissions.contains(Permission.ADMINISTRATOR)
}
