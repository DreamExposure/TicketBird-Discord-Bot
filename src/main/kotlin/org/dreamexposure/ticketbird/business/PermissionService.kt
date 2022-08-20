package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import discord4j.core.`object`.PermissionOverwrite
import discord4j.rest.util.PermissionSet

interface PermissionService {

    suspend fun checkingMissingBasePermissionsBot(guildId: Snowflake): PermissionSet

    fun getBotPermissionOverrides(): PermissionSet

    fun getSupportChannelMemberOverrides(): PermissionSet

    fun getTicketGrantOverrides(): PermissionSet

    fun getTicketChannelOverwrites(guildId: Snowflake, creator: Snowflake, staff: List<Snowflake>): List<PermissionOverwrite>

    fun hasRequiredElevatedPermissions(memberPermissions: PermissionSet): Boolean
}
