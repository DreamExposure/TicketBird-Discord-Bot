package org.dreamexposure.ticketbird.`object`

import discord4j.common.util.Snowflake
import org.dreamexposure.ticketbird.database.TicketData
import java.time.Instant

data class Ticket(
    val guildId: Snowflake,

    val number: Int,

    var project: String = "N/a",

    val creator: Snowflake,

    val participants: List<Snowflake> = listOf(),

    val channel: Snowflake,

    var category: Snowflake,

    var lastActivity: Instant = Instant.now(),

    var transcriptSha256: String? = null,
    var attachmentsSha256: String? = null,
) {
    constructor(data: TicketData): this(
        guildId = Snowflake.of(data.guildId),
        number = data.number,
        project = data.project,
        creator = Snowflake.of(data.creator),
        participants = data.participants
            ?.split(",")
            ?.filter(String::isNotBlank)
            ?.map(Snowflake::of)
            ?.toList() ?: listOf(),
        channel = Snowflake.of(data.channel),
        category = Snowflake.of(data.category),
        lastActivity = Instant.ofEpochMilli(data.lastActivity),
        transcriptSha256 = data.transcriptSha256,
        attachmentsSha256 = data.attachmentsSha256,
    )
}
