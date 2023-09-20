package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import discord4j.core.`object`.PermissionOverwrite
import discord4j.rest.util.PermissionSet
import org.dreamexposure.ticketbird.`object`.GuildSettings
import org.dreamexposure.ticketbird.`object`.Project

interface PermissionService {

    suspend fun checkingMissingBasePermissionsBot(guildId: Snowflake): PermissionSet


    fun getBotPermissionOverrides(): PermissionSet

    fun getSupportChannelMemberOverrides(): PermissionSet

    fun getTicketGrantOverrides(write: Boolean): PermissionSet

    fun getTicketChannelOverwrites(settings: GuildSettings, creator: Snowflake, project: Project?): List<PermissionOverwrite>

    fun getTicketParticipantGrantOverwrites(write: Boolean, user: Snowflake): PermissionOverwrite

    fun getTicketParticipantDenyOverwrites(user: Snowflake): PermissionOverwrite

    fun hasRequiredElevatedPermissions(memberPermissions: PermissionSet): Boolean
}
