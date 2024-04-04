package org.dreamexposure.ticketbird.database

import org.springframework.data.relational.core.mapping.Table

@Table("guild_settings")
data class GuildSettingsData(
    val guildId: Long,

    val lang: String,

    val patronGuild: Boolean,

    val devGuild: Boolean,

    val useProjects: Boolean,
    val enableLogging: Boolean,
    val showTicketStats: Boolean,
    val autoCloseHours: Int,
    val autoDeleteHours: Int,

    val requiresRepair: Boolean,

    val awaitingCategory: Long?,
    val respondedCategory: Long?,
    val holdCategory: Long?,
    val closeCategory: Long?,

    val supportChannel: Long?,
    val logChannel: Long?,

    val staticMessage: Long?,
    val staticMessageTitle: String?,
    val staticMessageDescription: String?,

    val nextId: Int,

    val staff: String,
    val staffRole: Long?,
    val pingOption: Int,
)
