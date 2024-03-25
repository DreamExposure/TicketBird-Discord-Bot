package org.dreamexposure.ticketbird.`object`

import discord4j.common.util.Snowflake
import org.dreamexposure.ticketbird.database.GuildSettingsData
import org.dreamexposure.ticketbird.extensions.handleLocaleDebt
import org.dreamexposure.ticketbird.extensions.listFromDb
import org.dreamexposure.ticketbird.extensions.toSnowflake
import java.time.Duration
import java.util.*

data class GuildSettings(
    val guildId: Snowflake,
    val locale: Locale = Locale.ENGLISH,
    val patronGuild: Boolean = false,
    val devGuild: Boolean = false,

    val useProjects: Boolean = false,
    val enableLogging: Boolean = false,
    val showTicketStats: Boolean = true,
    val autoClose: Duration = Duration.ofDays(7),
    val autoDelete: Duration = Duration.ofHours(24),

    val requiresRepair: Boolean = false,

    val awaitingCategory: Snowflake? = null,
    val respondedCategory: Snowflake? = null,
    val holdCategory: Snowflake? = null,
    val closeCategory: Snowflake? = null,
    val supportChannel: Snowflake? = null,
    val logChannel: Snowflake? = null,

    val staticMessage: Snowflake? = null,

    val nextId: Int = 1,
    val staff: Set<String> = setOf(),
    val staffRole: Snowflake? = null,
    val pingOption: PingOption = PingOption.AUTHOR_ONLY,
) {
    constructor(data: GuildSettingsData) : this(
        guildId = Snowflake.of(data.guildId),
        locale = data.lang.handleLocaleDebt(),
        patronGuild = data.patronGuild,
        useProjects = data.useProjects,
        enableLogging = data.enableLogging,
        showTicketStats = data.showTicketStats,
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
        staff = data.staff.listFromDb().toSet(),
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
            fun valueOf(value: Int) = entries.first { it.value == value }
        }
    }
}
