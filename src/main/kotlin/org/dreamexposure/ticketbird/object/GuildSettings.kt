package org.dreamexposure.ticketbird.`object`

import discord4j.common.util.Snowflake
import org.dreamexposure.ticketbird.database.GuildSettingsData
import org.dreamexposure.ticketbird.extensions.handleLocaleDebt
import org.dreamexposure.ticketbird.extensions.listFromDb
import org.dreamexposure.ticketbird.extensions.toSnowflake
import java.time.Duration
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

data class GuildSettings(
    val guildId: Snowflake,
    var locale: Locale = Locale.ENGLISH,
    var patronGuild: Boolean = false,
    var devGuild: Boolean = false,

    var useProjects: Boolean = false,
    var enableLogging: Boolean = false,
    var autoClose: Duration = Duration.ofDays(7),
    var autoDelete: Duration = Duration.ofHours(24),

    var requiresRepair: Boolean = false,

    var awaitingCategory: Snowflake? = null,
    var respondedCategory: Snowflake? = null,
    var holdCategory: Snowflake? = null,
    var closeCategory: Snowflake? = null,
    var supportChannel: Snowflake? = null,
    var logChannel: Snowflake? = null,

    var staticMessage: Snowflake? = null,

    var nextId: Int = 1,
    val staff: MutableList<String> = CopyOnWriteArrayList(),
    var staffRole: Snowflake? = null,
    var pingOption: PingOption = PingOption.AUTHOR_ONLY,
) {
    constructor(data: GuildSettingsData) : this(
        guildId = Snowflake.of(data.guildId),
        locale = data.lang.handleLocaleDebt(),
        patronGuild = data.patronGuild,
        useProjects = data.useProjects,
        enableLogging = data.enableLogging,
        autoClose = Duration.ofHours(data.autoCloseHours.toLong()),
        autoDelete = Duration.ofHours(data.autoDeleteHours.toLong()),


        requiresRepair = data.requiresRepair,

        awaitingCategory = data.awaitingCategory?.toSnowflake(),
        respondedCategory = data.respondedCategory?.toSnowflake(),
        holdCategory = data.holdCategory?.toSnowflake(),
        closeCategory = data.closeCategory?.toSnowflake(),
        supportChannel = data.supportChannel?.toSnowflake(),
        logChannel = data.logChannel?.toSnowflake(),

        staticMessage = data.staticMessage?.toSnowflake(),

        nextId = data.nextId,
        staff = data.staff.listFromDb(),
        staffRole = data.staffRole?.toSnowflake(),
        pingOption = PingOption.valueOf(data.pingOption)
    )

    fun hasRequiredIdsSet(): Boolean {
        return awaitingCategory != null
            && respondedCategory != null
            && holdCategory != null
            && closeCategory != null
            && supportChannel != null
            && staticMessage != null
    }

    enum class PingOption(val value: Int, val localeEntry: String) {
        AUTHOR_ONLY(1, "env.ping-option.author"),
        AUTHOR_AND_PROJECT_STAFF(2, "env.ping-option.author-project-staff"),
        AUTHOR_AND_ALL_STAFF(3, "env.ping-option.author-all-staff");

        companion object {
            fun valueOf(value: Int) = values().first { it.value == value }

        }
    }
}
