package org.dreamexposure.ticketbird.database

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("guild_settings")
data class GuildSettingsData(
    @Id
    val guildId: Long,

    var lang: String,

    var prefix: String,

    var patronGuild: Boolean,

    var devGuild: Boolean,

    var useProjects: Boolean,

    var awaitingCategory: Long?,
    var respondedCategory: Long?,
    var holdCategory: Long?,
    var closeCategory: Long?,

    var supportChannel: Long?,
    var staticMessage: Long?,

    var nextId: Int,

    var closedTotal: Int,

    val staff: String,
)
