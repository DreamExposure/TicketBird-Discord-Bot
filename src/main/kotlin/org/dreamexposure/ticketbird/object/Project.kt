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
) {
    constructor(data: ProjectData): this(
        id = data.id!!,
        guildId = Snowflake.of(data.guildId),
        name = data.projectName,
        prefix = data.projectPrefix,
        staffUsers = data.staffUsers
            ?.split(",")
            ?.filter(String::isNullOrEmpty)
            ?.map(Snowflake::of)
            ?.toList() ?: listOf(),
        staffRoles = data.staffRoles
            ?.split(",")
            ?.filter(String::isNullOrEmpty)
            ?.map(Snowflake::of)
            ?.toList() ?: listOf(),
    )
}
