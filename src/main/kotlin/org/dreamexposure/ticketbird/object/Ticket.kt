package org.dreamexposure.ticketbird.`object`

import discord4j.common.util.Snowflake

data class Ticket(
    val guildId: Snowflake,

    val number: Int,

    val project: String = "N/a",

    val creator: Snowflake,

    val channel: Snowflake,

    var category: Snowflake,

    var lastActivity: Long = System.currentTimeMillis(),
)
