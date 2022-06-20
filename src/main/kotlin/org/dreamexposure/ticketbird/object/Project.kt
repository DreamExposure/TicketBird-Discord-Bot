package org.dreamexposure.ticketbird.`object`

import discord4j.common.util.Snowflake
import org.dreamexposure.ticketbird.database.ProjectData

data class Project(
    val guildId: Snowflake,

    val name: String,

    val prefix: String,
) {
    constructor(data: ProjectData): this(
        guildId = Snowflake.of(data.guildId),
        name = data.projectName,
        prefix = data.projectPrefix,
    )
}
