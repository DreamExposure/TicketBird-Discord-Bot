package org.dreamexposure.ticketbird.`object`

import discord4j.common.util.Snowflake
import org.dreamexposure.ticketbird.database.GuildSettingsData
import org.dreamexposure.ticketbird.extensions.handleLocaleDebt
import org.dreamexposure.ticketbird.extensions.listFromDb
import org.dreamexposure.ticketbird.extensions.toSnowflake
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

data class GuildSettings(
    val guildId: Snowflake,
    var locale: Locale = Locale.ENGLISH,
    var patronGuild: Boolean = false,
    var devGuild: Boolean = false,
    var useProjects: Boolean = false,

    var awaitingCategory: Snowflake? = null,
    var respondedCategory: Snowflake? = null,
    var holdCategory: Snowflake? = null,
    var closeCategory: Snowflake? = null,
    var supportChannel: Snowflake? = null,
    var staticMessage: Snowflake? = null,

    var nextId: Int = 1,
    val staff: MutableList<String> = CopyOnWriteArrayList(),
) {
    constructor(data: GuildSettingsData) : this(
        guildId = Snowflake.of(data.guildId),
        locale = data.lang.handleLocaleDebt(),
        patronGuild = data.patronGuild,
        useProjects = data.useProjects,

        awaitingCategory = data.awaitingCategory?.toSnowflake(),
        respondedCategory = data.respondedCategory?.toSnowflake(),
        holdCategory = data.holdCategory?.toSnowflake(),
        closeCategory = data.closeCategory?.toSnowflake(),
        supportChannel = data.supportChannel?.toSnowflake(),
        staticMessage = data.staticMessage?.toSnowflake(),

        nextId = data.nextId,
        staff = data.staff.listFromDb()
    )
}
