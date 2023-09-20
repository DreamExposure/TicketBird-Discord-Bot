package org.dreamexposure.ticketbird.database

import org.springframework.data.relational.core.mapping.Table

@Table("tickets")
data class TicketData(
    val guildId: Long,
    val number: Int,
    val project: String,

    val creator: Long,
    val participants: String?,
    val channel: Long,
    var category: Long,

    val lastActivity: Long,

    val transcriptSha256: String?,
    val attachmentsSha256: String?,
)
