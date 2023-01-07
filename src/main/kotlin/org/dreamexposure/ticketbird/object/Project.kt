package org.dreamexposure.ticketbird.`object`

import discord4j.common.util.Snowflake
import org.dreamexposure.ticketbird.database.ProjectData

data class Project(
    val id: Long = 0,

    val guildId: Snowflake,
    val name: String,
    val prefix: String,
    val staffUsers: List<Snowflake> = listOf(),
    val staffRoles: List<Snowflake> = listOf(),
    val pingOverride: PingOverride = PingOverride.NONE,
) {
    constructor(data: ProjectData): this(
        id = data.id!!,
        guildId = Snowflake.of(data.guildId),
        name = data.projectName,
        prefix = data.projectPrefix,
        staffUsers = data.staffUsers
            ?.split(",")
            ?.filter(String::isNotBlank)
            ?.map(Snowflake::of)
            ?.toList() ?: listOf(),
        staffRoles = data.staffRoles
            ?.split(",")
            ?.filter(String::isNotBlank)
            ?.map(Snowflake::of)
            ?.toList() ?: listOf(),
        pingOverride = PingOverride.valueOf(data.pingOverride)
    )


    enum class PingOverride(val value: Int, val localeEntry: String) {
        NONE(1, "env.ping-override.none"),
        AUTHOR_ONLY(2, "env.ping-option.author"),
        AUTHOR_AND_PROJECT_STAFF(3, "env.ping-option.author-project-staff"),
        AUTHOR_AND_ALL_STAFF(4, "env.ping-option.author-all-staff");

        companion object {
            fun valueOf(value: Int) = values().first { it.value == value }
        }
    }
}
