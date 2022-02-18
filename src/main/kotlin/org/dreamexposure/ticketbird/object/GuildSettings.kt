package org.dreamexposure.ticketbird.`object`

import discord4j.common.util.Snowflake
import java.util.concurrent.CopyOnWriteArrayList

data class GuildSettings(
    val guildId: Snowflake,

    var lang: String = "English",

    var prefix: String = "=",

    var patronGuild: Boolean = false,

    var devGuild: Boolean = false,

    var useProjects: Boolean = false,

    var awaitingCategory: Snowflake? = null,
    var respondedCategory: Snowflake? = null,
    var holdCategory: Snowflake? = null,
    var closeCategory: Snowflake? = null,

    var supportChannel: Snowflake? = null,
    var staticMessage: Snowflake? = null,

    var nextId: Int = 1,

    var totalClosed: Int = 0,

    val staff: MutableList<String> = CopyOnWriteArrayList(),
) {
    // Constructor for Java classes to use
    constructor(guildId: Snowflake) : this(guildId, staff = CopyOnWriteArrayList())
}
