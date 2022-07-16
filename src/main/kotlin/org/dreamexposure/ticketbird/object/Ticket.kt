package org.dreamexposure.ticketbird.`object`

import discord4j.common.util.Snowflake
import org.dreamexposure.ticketbird.database.TicketData
import java.time.Instant

data class Ticket(
    val guildId: Snowflake,

    val number: Int,

    val project: String,

    val creator: Snowflake,

    val channel: Snowflake,

    var category: Snowflake,

    var lastActivity: Instant,
) {
    constructor(data: TicketData): this(
        guildId = Snowflake.of(data.guildId),
        number = data.number,
        project = data.project,
        creator = Snowflake.of(data.creator),
        channel = Snowflake.of(data.channel),
        category = Snowflake.of(data.category),
        lastActivity = Instant.ofEpochMilli(data.lastActivity),
    )
}
