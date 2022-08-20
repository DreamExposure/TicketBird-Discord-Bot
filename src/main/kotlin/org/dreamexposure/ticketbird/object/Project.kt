package org.dreamexposure.ticketbird.`object`

import discord4j.common.util.Snowflake
import org.dreamexposure.ticketbird.database.ProjectData

data class Project(
    val id: Long = 0,

    val guildId: Snowflake,

    val name: String,

    val prefix: String,
) {
    constructor(data: ProjectData): this(
        id = data.id!!,
        guildId = Snowflake.of(data.guildId),
        name = data.projectName,
        prefix = data.projectPrefix,
    )
}
