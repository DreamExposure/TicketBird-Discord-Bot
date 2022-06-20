package org.dreamexposure.ticketbird.database

import org.springframework.data.relational.core.mapping.Table

@Table("tickets")
data class TicketData(
    val guildId: Long,
    val number: Int,
    val project: String = "N/a",

    val creator: Long,
    val channel: Long,
    var category: Long,

    var lastActivity: Long = System.currentTimeMillis(),
)
