package org.dreamexposure.ticketbird.`object`

import discord4j.common.util.Snowflake

data class Project(
    val guildId: Snowflake,

    val name: String,

    val prefix: String,
)
