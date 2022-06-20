package org.dreamexposure.ticketbird.database

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("guild_settings")
data class GuildSettingsData(
    @Id
    val guildId: Long,

    var lang: String = "English",

    var prefix: String = "=",

    var patronGuild: Boolean = false,

    var devGuild: Boolean = false,

    var useProjects: Boolean = false,

    var awaitingCategory: Long? = null,
    var respondedCategory: Long? = null,
    var holdCategory: Long? = null,
    var closeCategory: Long? = null,

    var supportChannel: Long? = null,
    var staticMessage: Long? = null,

    var nextId: Int = 1,

    var closedTotal: Int = 0,

    val staff: String = "",
)
