package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import discord4j.core.`object`.PermissionOverwrite
import discord4j.rest.util.PermissionSet
import org.dreamexposure.ticketbird.`object`.GuildSettings

interface PermissionService {

    suspend fun checkingMissingBasePermissionsBot(guildId: Snowflake): PermissionSet

    fun getBotPermissionOverrides(): PermissionSet

    fun getSupportChannelMemberOverrides(): PermissionSet

    fun getTicketGrantOverrides(): PermissionSet

    fun getTicketChannelOverwrites(settings: GuildSettings, creator: Snowflake): List<PermissionOverwrite>

    fun hasRequiredElevatedPermissions(memberPermissions: PermissionSet): Boolean
}
